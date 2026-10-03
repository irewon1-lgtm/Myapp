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

    def visible(snapshot):
        # Reflow changes page numbers. Check the saved character's viewport instead.
        return page.evaluate("""saved => {
            const R = CB.Reader, block = R.blocks.find(n => n.dataset.block === saved.anchor);
            if (!block || !block.getClientRects().length) return false;
            const r = R.characterRect(block, saved.offset), v = R.viewport.getBoundingClientRect();
            return r.right > v.left - 2 && r.left < v.right + 2 &&
                   r.bottom > v.top - 3 && r.top < v.bottom + 3;
        }""", snapshot)

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
        check('comfort: actual SVG and caption use the dark reading theme',
              svg_style['loaded'] and 'invert(' in svg_style['filter'] and
              svg_style['background'] == svg_style['paper'] and
              svg_style['caption'] not in (None, 'rgb(47, 50, 46)'), svg_style)
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
        check('comfort: switching back to light removes dark image treatment',
              'brightness(' not in light_style['filter'] and
              'invert(' not in light_style['filter'], light_style)
        page.evaluate("CB.setPref('theme','dark')")
        open_book(books['svg']['book'])

        layouts = []
        for width in (360, 412, 800, 1280):
            focus_off()
            page.set_viewport_size({'width': width, 'height': 915})
            page.wait_for_timeout(300)
            before = page.evaluate('CB.Reader.viewport.clientHeight')
            # Capture and toggle in the same task, before goPage's 180ms save timer.
            snapshot = page.evaluate("""() => {
                const R=CB.Reader; R.goPage(Math.max(1, Math.floor(R.pages*.4)));
                const saved=R.info(); document.querySelector('#reader-focus-toggle').click();
                return saved;
            }""")
            page.wait_for_timeout(300)
            state = page.evaluate("""() => ({
                focus:CB.Reader.focus, height:CB.Reader.viewport.clientHeight,
                header:getComputedStyle(document.querySelector('.reader-header')).display,
                footer:getComputedStyle(document.querySelector('.reader-footer')).display,
                end:getComputedStyle(document.querySelector('.end-note')).display,
                toast:getComputedStyle(document.querySelector('.toast-region')).display,
                fits:document.documentElement.scrollWidth<=innerWidth+1,
                target:document.querySelector('#reader-focus-toggle').getBoundingClientRect().width
            })""")
            state.update(width=width, before=before, anchorVisible=visible(snapshot))
            layouts.append(state)
        check('comfort: all four widths hide chrome, expand content and fit the screen',
              all(s['focus'] and s['header'] == s['footer'] == s['end'] == s['toast'] == 'none'
                  and s['height'] > s['before'] and s['fits'] and s['target'] >= 44
                  for s in layouts), layouts)
        check('comfort: immediate page-turn focus preserves the visible reading anchor',
              all(s['anchorVisible'] for s in layouts), layouts)

        focus_off()
        page.set_viewport_size({'width': 412, 'height': 915})
        page.wait_for_timeout(300)
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
            // Exclude the book reflow performed synchronously inside the click.
            started=performance.now();
            setTimeout(() => {before={elapsed:performance.now()-started,
                hidden:CB.Reader.focusHidden,opacity:getComputedStyle(button).opacity};},4800);
            setTimeout(() => {observer.disconnect();resolve({before,hiddenAt,
                hidden:CB.Reader.focusHidden,opacity:getComputedStyle(button).opacity,
                focus:CB.Reader.focus});},5400);
        })""")
        check('comfort: button is visible before 5s and hides at the 5s boundary',
              timing['before'] is not None and timing['before']['elapsed'] < 5000 and
              not timing['before']['hidden'] and timing['before']['opacity'] == '1' and
              timing['hiddenAt'] is not None and 4980 <= timing['hiddenAt'] <= 5400 and
              timing['hidden'] and timing['opacity'] == '0' and timing['focus'], timing)
        page.screenshot(path=str(out / 'comfort-focus-hidden-412.png'), full_page=True)
        number = page.evaluate('CB.Reader.page')
        rect = button.bounding_box()
        page.mouse.click(rect['x'] + rect['width']/2, rect['y'] + rect['height']/2)
        revealed = page.evaluate("""() => ({focus:CB.Reader.focus, hidden:CB.Reader.focusHidden,
            page:CB.Reader.page,opacity:getComputedStyle(document.querySelector('#reader-focus-toggle')).opacity})""")
        check('comfort: first tap at hidden position only reveals the control',
              revealed['focus'] and not revealed['hidden'] and
              revealed['page'] == number and revealed['opacity'] == '1', revealed)
        page.mouse.click(rect['x'] + rect['width']/2, rect['y'] + rect['height']/2)
        page.wait_for_timeout(300)
        check('comfort: second tap exits and restores reading controls',
              page.evaluate("""!CB.Reader.focus && !CB.Reader.focusHidden &&
                  getComputedStyle(document.querySelector('.reader-header')).display!=='none' &&
                  getComputedStyle(document.querySelector('.reader-footer')).display!=='none'"""))

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
        scroll_snapshot = page.evaluate('CB.Reader.info()')
        focus_on()
        scroll_preserved = visible(scroll_snapshot)
        top = page.evaluate('CB.Reader.viewport.scrollTop')
        viewport = page.locator('#reader-viewport').bounding_box()
        page.mouse.move(viewport['x']+viewport['width']/2, viewport['y']+viewport['height']/2)
        page.mouse.wheel(0, 340)
        page.wait_for_timeout(350)
        moved = page.evaluate('CB.Reader.viewport.scrollTop') > top + 20
        scroll_exit = page.evaluate('CB.Reader.info()')
        page.keyboard.press('Escape')
        page.wait_for_timeout(300)
        check('comfort: scroll mode keeps its reading anchor and scrolls in focus',
              scroll_preserved and moved and visible(scroll_exit) and
              page.evaluate('!CB.Reader.focus'),
              {'entryAnchorVisible': scroll_preserved, 'wheelMoved': moved,
               'exitAnchorVisible': visible(scroll_exit)})

        page.evaluate("CB.setPref('readingMode','page'); CB.Reader.applySettings()")
        page.wait_for_timeout(350)
        end_before = page.evaluate("""() => {
            const R=CB.Reader; R.goAnchor(R.book.id+'-end'); return R.page;
        }""")
        page.wait_for_timeout(250)
        focus_on()
        end_after = page.evaluate("""() => ({page:CB.Reader.page,pages:CB.Reader.pages,
            anchor:CB.Reader.info().anchor,focus:CB.Reader.focus,
            display:getComputedStyle(document.querySelector('.end-note')).display})""")
        check('comfort: entering focus from completion stays at the end of the book',
              end_before > 0 and end_after['focus'] and end_after['display'] == 'none' and
              end_after['page'] > 0 and end_after['page'] >= end_after['pages'] - 2,
              {'before': end_before, 'after': end_after})

        # Leave with a live hide timer, then use a second real interval to catch stale callbacks.
        page.evaluate('CB.Reader.showFocusButton(); CB.go("library")')
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
        check('comfort: focus toggle remains reachable with a visible keyboard focus',
              keyboard['focused'] and keyboard['visible'] == '1' and
              keyboard['outline'] != 'none', keyboard)
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
