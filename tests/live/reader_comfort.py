"""Focused Chromium checks against real catalog books and the current reader.

The caller supplies a disposable, unsigned-in browser page. This module neither
replaces content nor mocks synchronization, image responses, or the global clock.
Only normal reading preferences/progress change. Screenshots go to the caller's
review directory; the original book, theme, and viewport are restored afterward.
"""

from pathlib import Path
import time


def verify_reader_comfort(page, check, out):
    """Run 18 grouped reader checks; ``check(name, ok, detail=None)`` records them."""
    out = Path(out)
    out.mkdir(parents=True, exist_ok=True)
    original_viewport = page.viewport_size
    initial = page.evaluate("""() => ({
        book: CB.Reader.alive ? CB.Reader.book.id : CB.selected().id,
        position: CB.Reader.alive && CB.Reader.measured ? CB.Reader.info() : null,
        theme: CB.pref('theme', 'cream'), motion: CB.pref('motion', false),
        unsigned: !CB.secret(),
        content: JSON.stringify(CB.catalog),
        annotations: JSON.stringify(CB.all('annotation:'))
    })""")
    books = page.evaluate("""() => {
        const images = CB.catalog.books.flatMap(book => book.chapters.flatMap(ch =>
            (ch.blocks || []).filter(bl => bl.type === 'image').map(bl => ({
                book: book.id, title: book.title, block: bl.id, src: CB.art(bl.src)
            }))));
        const svg = images.find(im => /api/i.test(im.book) &&
            im.src.startsWith('data:image/svg+xml')) || images.find(im =>
            /python|파이썬/i.test(im.book + im.title) &&
            im.src.startsWith('data:image/svg+xml'));
        const photo = images.find(im => !im.src.startsWith('data:image/svg+xml') &&
            !/^https?:/i.test(im.src)) || images.find(im =>
            /api/i.test(im.book) && !im.src.startsWith('data:image/svg+xml'));
        const brief = im => im ? {book: im.book, block: im.block, title: im.title} : null;
        return {svg: brief(svg), photo: brief(photo)};
    }""")
    check('comfort: real teaching books and unsigned disposable profile',
          initial['unsigned'] and bool(books['svg']) and bool(books['photo']),
          {'svg': books['svg'], 'photo': books['photo'], 'unsigned': initial['unsigned']})
    if not initial['unsigned'] or not books['svg'] or not books['photo']:
        raise AssertionError('Reader checks require existing images and an unsigned test profile')

    def wait_js(expression, arg=None, timeout=15000):
        # wait_for_function compiles through eval, forbidden by the real CSP.
        deadline = time.monotonic() + timeout / 1000
        last_error = None
        while time.monotonic() < deadline:
            try:
                if page.evaluate(expression, arg):
                    return
            except Exception as error:
                last_error = error
            page.wait_for_timeout(100)
        raise AssertionError('Reader comfort condition timed out: ' + expression + '; ' + str(last_error))

    def open_book(book, anchor=None, offset=0):
        page.evaluate("""arg => CB.read(arg.book, arg.anchor, arg.offset)""",
                      {'book': book, 'anchor': anchor, 'offset': offset})
        wait_js("""id => CB.Reader.alive && CB.Reader.measured &&
            CB.Reader.book.id === id && CB.Reader.pages > 1""", arg=book)
        page.wait_for_timeout(300)

    def show_image(image):
        open_book(image['book'], image['block'])
        selector = 'figure[data-block="' + image['block'] + '"]'
        wait_js("""selector => {
            const im = document.querySelector(selector + ' img');
            return im && im.complete && im.naturalWidth > 0;
        }""", arg=selector, timeout=15000)
        return selector

    def geometry(reference=None):
        # The same character must keep its screen coordinates, not merely remain visible.
        return page.evaluate("""saved => {
            const R=CB.Reader, info=saved||R.info();
            const block=R.blocks.find(n => n.dataset.block===info.anchor);
            const rect=r=>({x:r.x,y:r.y,width:r.width,height:r.height});
            return {reference:{anchor:info.anchor,offset:info.offset||0},
                page:R.page,pages:R.pages,step:R.step,
                viewport:rect(R.viewport.getBoundingClientRect()),
                clientWidth:R.viewport.clientWidth,clientHeight:R.viewport.clientHeight,
                scrollTop:R.viewport.scrollTop,scrollHeight:R.viewport.scrollHeight,
                styleTransform:R.flow.style.transform,transform:getComputedStyle(R.flow).transform,
                documentX:scrollX,documentY:scrollY,
                character:block?rect(R.characterRect(block,info.offset||0)):null};
        }""", reference)

    def changes(before, after):
        changed = []
        for key in ('page', 'pages', 'step', 'clientWidth', 'clientHeight', 'scrollHeight',
                    'styleTransform', 'transform', 'documentX', 'documentY'):
            if before[key] != after[key]:
                changed.append(key)
        if abs(before['scrollTop'] - after['scrollTop']) > 0.05:
            changed.append('scrollTop')
        for key in ('viewport', 'character'):
            if before[key] is None or after[key] is None:
                changed.append(key + '.missing')
            else:
                for axis in ('x', 'y', 'width', 'height'):
                    if abs(before[key][axis] - after[key][axis]) > 0.05:
                        changed.append(key + '.' + axis)
        return changed

    def wait_reader_settled():
        # Finish a preceding page turn/resize before taking the focus baseline.
        # Samples after the focus action remain immediate and keep the same tolerance.
        wait_js("""() => {
            const R=CB.Reader;
            if(!R.alive || !R.measured || R.measuring ||
               R.width!==R.viewport.clientWidth || R.height!==R.viewport.clientHeight) return false;
            if(R.flow.getAnimations().some(a=>a.pending || a.playState==='running')) return false;
            if(R.mode()!=='page') return true;
            const transform=getComputedStyle(R.flow).transform;
            const x=transform==='none'?0:new DOMMatrixReadOnly(transform).m41;
            return Math.abs(x-(-R.page*R.step))<=0.05;
        }""")

    def transition(action):
        wait_reader_settled()
        before = geometry()
        action()
        immediate = geometry(before['reference'])
        page.evaluate('() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)))')
        framed = geometry(before['reference'])
        page.wait_for_timeout(250)
        settled = geometry(before['reference'])
        return {'before': before, 'after': settled,
                'changedImmediately': changes(before, immediate),
                'changedAfterFrames': changes(before, framed),
                'changedAfter250ms': changes(before, settled)}

    def unmoved(result):
        return not any(result[key] for key in
                       ('changedImmediately', 'changedAfterFrames', 'changedAfter250ms'))

    def chrome():
        return page.evaluate("""() => ({
            focus:CB.Reader.focus,
            hidden:['.reader-header','.reader-footer','.end-note'].map(selector=>{
                const el=document.querySelector(selector),s=getComputedStyle(el);
                return {selector,visibility:s.visibility,display:s.display,inert:el.inert};
            }),toast:getComputedStyle(document.querySelector('.toast-region')).display,
            fits:document.documentElement.scrollWidth<=innerWidth+1,
            target:document.querySelector('#reader-focus-toggle').getBoundingClientRect().width
        })""")

    def theme_state():
        return page.evaluate("""() => ({
            rootTheme:document.documentElement.dataset.theme,bodyTheme:document.body.dataset.theme,
            colors:['html','body','.reader','.reader-footer'].map(s=>getComputedStyle(document.querySelector(s)).backgroundColor),
            bodyImage:getComputedStyle(document.body).backgroundImage,
            scheme:getComputedStyle(document.documentElement).colorScheme,
            meta:document.querySelector('meta[name="theme-color"]')?.content
        })""")

    def focus_on():
        if not page.evaluate('CB.Reader.focus'):
            page.locator('#reader-focus-toggle').click()
        page.wait_for_timeout(250)

    def focus_off():
        page.evaluate('CB.Reader.setFocus(false)')
        page.wait_for_timeout(250)

    def image_style(selector):
        return page.evaluate("""selector => {
            const el = document.querySelector(selector), im = el.querySelector('img');
            const caption = el.querySelector('figcaption') || document.querySelector('.reader-image-caption');
            return {filter: getComputedStyle(im).filter,
                background: getComputedStyle(el).backgroundColor,
                paper: getComputedStyle(document.querySelector('.reader')).backgroundColor,
                caption: caption ? getComputedStyle(caption).color : null,
                loaded: im.complete && im.naturalWidth > 0};
        }""", selector)

    try:
        page.set_viewport_size({'width': 412, 'height': 915})
        page.evaluate("CB.setPref('readingMode','page'); CB.setPref('theme','dark');")
        svg_selector = show_image(books['svg'])
        button = page.locator('#reader-focus-toggle')
        control = button.evaluate("""el => {
            const r = el.getBoundingClientRect();
            return {width:r.width, height:r.height, label:el.getAttribute('aria-label'),
                pressed:el.getAttribute('aria-pressed'),
                hint:document.getElementById(el.getAttribute('aria-describedby'))?.textContent,
                opacity:getComputedStyle(el).opacity};
        }""")
        check('comfort: small focus control has 44px target and accessible instructions',
              control['width'] >= 44 and control['height'] >= 44 and
              control['pressed'] == 'false' and bool(control['label']) and
              '5초' in (control['hint'] or '') and control['opacity'] == '1', control)

        svg_style = image_style(svg_selector)
        dark = theme_state()
        check('comfort: actual diagram and root/body/reader use pure black dark mode',
              svg_style['loaded'] and 'invert(' in svg_style['filter'] and
              svg_style['background'] == svg_style['paper'] and
              svg_style['caption'] not in (None, 'rgb(47, 50, 46)') and
              dark['rootTheme'] == dark['bodyTheme'] == 'dark' and
              all(color == 'rgb(0, 0, 0)' for color in dark['colors']) and
              dark['bodyImage'] == 'none' and dark['scheme'] == 'dark' and
              (dark['meta'] or '').lower() == '#000000', {'image': svg_style, 'theme': dark})
        page.screenshot(path=str(out / 'comfort-dark-diagram-412.png'), full_page=True)
        page.locator(svg_selector + ' button').click()
        page.wait_for_selector('.reader-image-modal img')
        modal_style = image_style('.reader-image-modal')
        check('comfort: enlarged actual diagram and caption keep the dark theme',
              modal_style['loaded'] and modal_style['filter'] == svg_style['filter'] and
              modal_style['background'] == modal_style['paper'] and
              bool(modal_style['caption']), modal_style)
        page.screenshot(path=str(out / 'comfort-dark-enlarged-412.png'), full_page=True)
        page.keyboard.press('Escape')

        photo_selector = show_image(books['photo'])
        photo_style = image_style(photo_selector)
        page.locator(photo_selector + ' button').click()
        page.wait_for_selector('.reader-image-modal img')
        photo_modal = image_style('.reader-image-modal')
        check('comfort: actual photo dims without color inversion, including enlargement',
              photo_style['loaded'] and photo_modal['loaded'] and
              'brightness(' in photo_style['filter'] and 'invert(' not in photo_style['filter'] and
              photo_modal['filter'] == photo_style['filter'],
              {'inline': photo_style, 'enlarged': photo_modal})
        page.keyboard.press('Escape')
        page.evaluate("CB.setPref('theme','light')")
        light_style = image_style(photo_selector)
        light = theme_state()
        check('comfort: light mode restores root/system colors and normal image treatment',
              'brightness(' not in light_style['filter'] and
              'invert(' not in light_style['filter'] and
              light['rootTheme'] == light['bodyTheme'] == 'light' and
              light['scheme'] == 'light' and bool(light['meta']) and
              light['meta'].lower() != '#000000',
              {'image': light_style, 'theme': light})
        page.evaluate("CB.setPref('theme','dark')")
        open_book(books['svg']['book'])

        layouts = []
        for width in (360, 412, 800, 1280):
            focus_off()
            page.set_viewport_size({'width': width, 'height': 915})
            page.wait_for_timeout(300)
            page.evaluate('CB.Reader.goPage(Math.max(1,Math.floor(CB.Reader.pages*.4)))')
            # Complete the requested page turn before measuring focus-only movement.
            page.wait_for_timeout(300)
            entered = transition(lambda: button.click())
            hidden = chrome()
            exited = transition(lambda: button.click())
            restored = chrome()
            layouts.append({'width': width, 'entry': entered, 'exit': exited,
                            'hidden': hidden, 'restored': restored})
        check('comfort: four widths hide controls without moving any text or page',
              all(unmoved(s['entry']) and unmoved(s['exit']) and s['hidden']['focus'] and
                  all(el['visibility'] == 'hidden' and el['display'] != 'none' and el['inert']
                      for el in s['hidden']['hidden']) and
                  s['hidden']['toast'] == 'none' and s['hidden']['fits'] and
                  s['hidden']['target'] >= 44 and not s['restored']['focus'] and
                  all(el['visibility'] == 'visible' and not el['inert']
                      for el in s['restored']['hidden']) for s in layouts), layouts)

        # A pending normal page save must survive an immediate focus toggle.
        pending = page.evaluate("""() => {
            const R=CB.Reader, previous=CB.progress(R.book.id).page;
            R.goPage(Math.min(R.pages-2,R.page+2));
            const expected={page:R.page,pages:R.pages,step:R.step,transform:R.flow.style.transform};
            const beforeRecord=CB.progress(R.book.id).page;
            document.querySelector('#reader-focus-toggle').click();
            return {previous,beforeRecord,expected,immediate:{page:R.page,pages:R.pages,
                step:R.step,transform:R.flow.style.transform}};
        }""")
        page.wait_for_timeout(350)
        saved = page.evaluate("""() => ({page:CB.Reader.page,pages:CB.Reader.pages,
            step:CB.Reader.step,transform:CB.Reader.flow.style.transform,
            storedPage:CB.progress(CB.Reader.book.id).page,
            storedPages:CB.progress(CB.Reader.book.id).pages,focus:CB.Reader.focus})""")
        check('comfort: immediate focus keeps the target page and its pending save',
              pending['previous'] == pending['beforeRecord'] and
              pending['previous'] != pending['expected']['page'] and
              pending['immediate'] == pending['expected'] and saved['focus'] and
              all(saved[key] == pending['expected'][key] for key in pending['expected']) and
              saved['storedPage'] == pending['expected']['page'] and
              saved['storedPages'] == pending['expected']['pages'],
              {'pending': pending, 'saved': saved})

        focus_off()
        page.set_viewport_size({'width': 412, 'height': 915})
        page.wait_for_timeout(300)
        wait_reader_settled()
        timer_position = geometry()
        # One real 5.4-second interval. No clock install or application timer replacement.
        timing = page.evaluate("""() => new Promise(resolve => {
            const button=document.querySelector('#reader-focus-toggle');
            document.activeElement?.blur();
            let hiddenAt=null, before=null, started=null;
            const observer=new MutationObserver(() => {
                if (hiddenAt===null && button.classList.contains('is-hidden'))
                    hiddenAt=performance.now()-started;
            });
            observer.observe(button,{attributes:true,attributeFilter:['class']});
            button.click();
            // Start after event dispatch; no clock or application callback is replaced.
            started=performance.now();
            setTimeout(() => {before={elapsed:performance.now()-started,
                hidden:CB.Reader.focusHidden,opacity:getComputedStyle(button).opacity};},4800);
            setTimeout(() => {observer.disconnect();resolve({before,hiddenAt,
                hidden:CB.Reader.focusHidden,opacity:getComputedStyle(button).opacity,
                focus:CB.Reader.focus});},5400);
        })""")
        timer_after = geometry(timer_position['reference'])
        check('comfort: button hides at 5s without moving the text or page',
              timing['before'] is not None and timing['before']['elapsed'] < 5000 and
              not timing['before']['hidden'] and timing['before']['opacity'] == '1' and
              timing['hiddenAt'] is not None and 4980 <= timing['hiddenAt'] <= 5400 and
              timing['hidden'] and timing['opacity'] == '0' and timing['focus'] and
              not changes(timer_position, timer_after),
              {'timing': timing, 'before': timer_position, 'after': timer_after,
               'changed': changes(timer_position, timer_after)})
        page.screenshot(path=str(out / 'comfort-focus-hidden-412.png'), full_page=True)
        rect = button.bounding_box()
        revealed_position = transition(lambda: page.mouse.click(
            rect['x'] + rect['width']/2, rect['y'] + rect['height']/2))
        revealed = page.evaluate("""() => ({focus:CB.Reader.focus, hidden:CB.Reader.focusHidden,
            page:CB.Reader.page,opacity:getComputedStyle(document.querySelector('#reader-focus-toggle')).opacity})""")
        check('comfort: first tap only reveals the control and keeps exact reading geometry',
              revealed['focus'] and not revealed['hidden'] and
              revealed['opacity'] == '1' and unmoved(revealed_position),
              {'state': revealed, 'position': revealed_position})
        exited_position = transition(lambda: page.mouse.click(
            rect['x'] + rect['width']/2, rect['y'] + rect['height']/2))
        restored = chrome()
        check('comfort: second tap restores controls without moving the text or page',
              not restored['focus'] and unmoved(exited_position) and
              all(el['visibility'] == 'visible' and not el['inert']
                  for el in restored['hidden']),
              {'state': restored, 'position': exited_position})

        focus_on()
        number = page.evaluate('CB.Reader.page')
        page.locator('#reader-viewport').focus()
        page.keyboard.press('ArrowRight')
        page.wait_for_timeout(250)
        after_key = page.evaluate('CB.Reader.page')
        page.evaluate("""() => {
            const el=CB.Reader.viewport, r=el.getBoundingClientRect();
            const touch=new Touch({identifier:7,target:el,clientX:r.left+r.width*.1,
                clientY:r.top+r.height*.7});
            el.dispatchEvent(new TouchEvent('touchstart',{bubbles:true,touches:[touch],
                targetTouches:[touch],changedTouches:[touch]}));
            el.dispatchEvent(new TouchEvent('touchend',{bubbles:true,touches:[],
                targetTouches:[],changedTouches:[touch]}));
        }""")
        page.wait_for_timeout(250)
        check('comfort: keyboard and touch-edge page navigation remain active',
              after_key == number + 1 and page.evaluate('CB.Reader.page') == number and
              page.evaluate('CB.Reader.focus'), {'before': number, 'afterKeyboard': after_key})

        page.evaluate('(anchor)=>CB.Reader.goAnchor(anchor)', books['svg']['block'])
        page.wait_for_timeout(250)
        page.locator('figure[data-block="' + books['svg']['block'] + '"] button').click()
        page.wait_for_selector('.reader-image-modal img')
        page.keyboard.press('Escape')
        first_escape = page.evaluate('CB.Reader.focus && !CB.modal')
        page.keyboard.press('Escape')
        check('comfort: Escape closes image first and focus only on the next press',
              first_escape and page.evaluate('!CB.Reader.focus && !CB.modal'))

        page.evaluate("CB.setPref('readingMode','scroll'); CB.Reader.applySettings()")
        page.wait_for_timeout(350)
        page.evaluate("""() => {
            const R=CB.Reader, last=R.viewport.style.scrollBehavior;
            R.viewport.style.scrollBehavior='auto'; R.viewport.scrollTop=R.viewport.scrollHeight*.35;
            R.viewport.style.scrollBehavior=last;
        }""")
        page.wait_for_timeout(250)
        scroll_entered = transition(lambda: button.click())
        top = page.evaluate('CB.Reader.viewport.scrollTop')
        viewport = page.locator('#reader-viewport').bounding_box()
        page.mouse.move(viewport['x']+viewport['width']/2, viewport['y']+viewport['height']/2)
        page.mouse.wheel(0, 340)
        page.wait_for_timeout(350)
        moved = page.evaluate('CB.Reader.viewport.scrollTop') > top + 20
        scroll_exited = transition(lambda: page.keyboard.press('Escape'))
        page.evaluate("""() => {
            const R=CB.Reader,last=R.viewport.style.scrollBehavior;
            R.viewport.style.scrollBehavior='auto';R.viewport.scrollTop=R.viewport.scrollHeight;
            R.viewport.style.scrollBehavior=last;
        }""")
        page.wait_for_timeout(300)
        bottom_entered = transition(lambda: button.click())
        bottom_exited = transition(lambda: button.click())
        bottom = bottom_entered['before']
        check('comfort: scroll focus preserves exact middle/end positions and allows scrolling',
              moved and all(unmoved(result) for result in
                  (scroll_entered, scroll_exited, bottom_entered, bottom_exited)) and
              bottom['scrollTop'] > 0 and
              abs(bottom['scrollTop'] + bottom['clientHeight'] - bottom['scrollHeight']) <= 1 and
              page.evaluate('!CB.Reader.focus'),
              {'middleEntry': scroll_entered, 'middleExit': scroll_exited,
               'wheelMoved': moved, 'bottomEntry': bottom_entered, 'bottomExit': bottom_exited})

        page.evaluate("CB.setPref('readingMode','page'); CB.Reader.applySettings()")
        page.wait_for_timeout(350)
        page.evaluate('CB.Reader.goAnchor(CB.Reader.book.id+"-end")')
        page.wait_for_timeout(300)
        end_entered = transition(lambda: button.click())
        end_hidden = chrome()
        end_exited = transition(lambda: button.click())
        check('comfort: completion keeps exactly the same page, page count and position',
              end_entered['before']['page'] > 0 and unmoved(end_entered) and unmoved(end_exited) and
              all(el['visibility'] == 'hidden' and el['display'] != 'none' and el['inert']
                  for el in end_hidden['hidden']),
              {'entry': end_entered, 'exit': end_exited, 'hidden': end_hidden})

        # Leave with a live hide timer, then use a second real interval to catch stale callbacks.
        page.evaluate('CB.Reader.setFocus(true); CB.Reader.showFocusButton(); CB.go("library")')
        wait_js('!CB.Reader.alive && !document.body.classList.contains("reader-focus")')
        open_book(books['svg']['book'], books['svg']['block'])
        page.wait_for_timeout(5100)
        reentry = page.evaluate("""() => ({focus:CB.Reader.focus,hidden:CB.Reader.focusHidden,
            body:document.body.classList.contains('reader-focus'),
            opacity:getComputedStyle(document.querySelector('#reader-focus-toggle')).opacity})""")
        check('comfort: reader destruction clears focus and old hide callbacks',
              not reentry['focus'] and not reentry['hidden'] and not reentry['body'] and
              reentry['opacity'] == '1', reentry)
        page.locator('#reader-viewport').focus()
        page.keyboard.press('Shift+Tab')
        keyboard = page.locator('#reader-focus-toggle').evaluate("""el => ({
            focused:document.activeElement===el,visible:getComputedStyle(el).opacity,
            outline:getComputedStyle(el).outlineStyle})""")
        page.keyboard.press('Enter')
        inert = page.evaluate("""() => [...document.querySelectorAll(
            '.reader-header button,.reader-footer button,.end-note button')].map(el => {
            el.focus({preventScroll:true});return {action:el.dataset.action,
                focused:document.activeElement===el,inert:!!el.closest('[inert]')};
        })""")
        page.keyboard.press('Tab')
        check('comfort: keyboard reaches focus toggle and skips all hidden inert controls',
              keyboard['focused'] and keyboard['visible'] == '1' and
              keyboard['outline'] != 'none' and len(inert) > 0 and
              all(el['inert'] and not el['focused'] for el in inert) and
              page.evaluate('CB.Reader.focus && document.activeElement===CB.Reader.viewport'),
              {'keyboard': keyboard, 'hiddenControls': inert})
        page.keyboard.press('Escape')

        # One tablet reference image, without adding another viewport test matrix.
        page.set_viewport_size({'width': 960, 'height': 1536})
        open_book(books['svg']['book'], books['svg']['block'])
        focus_on()
        page.screenshot(path=str(out / 'comfort-black-focus-tablet-960.png'), full_page=True)
        focus_off()
        unchanged = page.evaluate("""initial => ({
            catalog:JSON.stringify(CB.catalog)===initial.content,
            annotations:JSON.stringify(CB.all('annotation:'))===initial.annotations,
            unsigned:!CB.secret()
        })""", initial)
        check('comfort: existing books and annotations remain unchanged',
              all(unchanged.values()), unchanged)
    finally:
        # No clock was installed. Restore ordinary reading for the surrounding suite.
        page.evaluate("""state => {
            if (CB.modal) CB.closeModal();
            CB.Reader.setFocus(false);
            CB.setPref('readingMode','page'); CB.setPref('theme',state.theme);
            CB.setPref('motion',state.motion);
            CB.read(state.book,state.position?.anchor,state.position?.offset||0);
        }""", initial)
        if original_viewport:
            page.set_viewport_size(original_viewport)
        wait_js('CB.Reader.alive && CB.Reader.measured && !CB.Reader.focus')
        page.wait_for_timeout(300)
