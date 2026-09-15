package com.futuretech.poweruser.sandbox

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RuntimeExtremeInstrumentedTest {
    private data class C(
        val id: String,
        val lang: ProgrammingLanguage,
        val code: String,
        val ok: Boolean,
        val out: String? = null,
        val err: String? = null,
        val sec: Boolean? = null,
        val timeout: Boolean? = null
    )

    @Test
    fun runtimeExtreme60() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val engine = SandboxedExecutionEngine(context)
        val cases = pythonCases() + typeScriptCases()
        val rows = JSONArray()
        val failures = mutableListOf<String>()
        for (c in cases) {
            val r = engine.execute(c.lang, c.code)
            var pass = r.isSuccess == c.ok
            c.out?.let { pass = pass && r.output.contains(it) }
            c.err?.let { pass = pass && (r.errorMessage ?: "").contains(it, ignoreCase = true) }
            c.sec?.let { pass = pass && r.isSecurityViolation == it }
            c.timeout?.let { pass = pass && r.isTimeout == it }
            rows.put(JSONObject().apply {
                put("id", c.id); put("language", c.lang.name); put("pass", pass)
                put("success", r.isSuccess); put("security", r.isSecurityViolation); put("timeout", r.isTimeout)
                put("output", r.output.take(500)); put("error", r.errorMessage ?: JSONObject.NULL); put("ms", r.executionTimeMs)
            })
            if (!pass) failures += "${c.id}: success=${r.isSuccess}, security=${r.isSecurityViolation}, timeout=${r.isTimeout}, output=${r.output.take(80)}, error=${r.errorMessage}"
        }
        val report = JSONObject().apply {
            put("suite", "RUNTIME_EXTREME_60")
            put("scope", "real Python and TypeScript execution engines")
            put("total", cases.size); put("passed", cases.size - failures.size); put("failed", failures.size); put("cases", rows)
        }
        context.openFileOutput("runtime_extreme_report.json", Context.MODE_PRIVATE).use { it.write(report.toString(2).toByteArray()) }
        assertTrue("Runtime Extreme60 failures:\n${failures.joinToString("\n")}", cases.size == 60 && failures.isEmpty())
    }

    private fun pythonCases() = listOf(
        C("RX-P01", ProgrammingLanguage.PYTHON, "print(2 + 3)", true, "5"),
        C("RX-P02", ProgrammingLanguage.PYTHON, "name='Kim'\nprint('Hi '+name)", true, "Hi Kim"),
        C("RX-P03", ProgrammingLanguage.PYTHON, "a=[10,20,30]\nprint(a[1])", true, "20"),
        C("RX-P04", ProgrammingLanguage.PYTHON, "d={'x':7}\nprint(d['x'])", true, "7"),
        C("RX-P05", ProgrammingLanguage.PYTHON, "x=9\nif x>5:\n    print('big')\nelse:\n    print('small')", true, "big"),
        C("RX-P06", ProgrammingLanguage.PYTHON, "s=0\nfor i in range(5):\n    s=s+i\nprint(s)", true, "10"),
        C("RX-P07", ProgrammingLanguage.PYTHON, "n=0\nwhile n<3:\n    n+=1\nprint(n)", true, "3"),
        C("RX-P08", ProgrammingLanguage.PYTHON, "def add(a,b):\n    return a+b\nprint(add(4,6))", true, "10"),
        C("RX-P09", ProgrammingLanguage.PYTHON, "x=12\nprint(f'value={x}')", true, "value=12"),
        C("RX-P10", ProgrammingLanguage.PYTHON, "a=[x*x for x in range(4)]\nprint(a)", true, "[0, 1, 4, 9]"),
        C("RX-P11", ProgrammingLanguage.PYTHON, "a=[1,2,3]\nprint(len(a), sum(a))", true, "3 6"),
        C("RX-P12", ProgrammingLanguage.PYTHON, "print(bool(1), bool(0))", true, "True False"),
        C("RX-P13", ProgrammingLanguage.PYTHON, "c=0\nfor i in range(3):\n    for j in range(2):\n        c+=1\nprint(c)", true, "6"),
        C("RX-P14", ProgrammingLanguage.PYTHON, "x=[]\nfor i in range(5):\n    if i==1: continue\n    if i==4: break\n    x.append(i)\nprint(x)", true, "[0, 2, 3]"),
        C("RX-P15", ProgrammingLanguage.PYTHON, "if True print('x')", false, err="SyntaxError"),
        C("RX-P16", ProgrammingLanguage.PYTHON, "print(missing_name)", false, err="NameError"),
        C("RX-P17", ProgrammingLanguage.PYTHON, "print(1/0)", false, err="ZeroDivisionError"),
        C("RX-P18", ProgrammingLanguage.PYTHON, "im" + "port os\nprint('x')", false, sec=true),
        C("RX-P19", ProgrammingLanguage.PYTHON, "im" + "port urllib\nprint('x')", false, sec=true),
        C("RX-P20", ProgrammingLanguage.PYTHON, "o" + "pen('practice.tmp','w')", false, sec=true),
        C("RX-P21", ProgrammingLanguage.PYTHON, "ex" + "ec('print(1)')", false, sec=true),
        C("RX-P22", ProgrammingLanguage.PYTHON, "print((1)." + "__class__)", false, sec=true),
        C("RX-P23", ProgrammingLanguage.PYTHON, "while True:\n    pass", false, timeout=true),
        C("RX-P24", ProgrammingLanguage.PYTHON, "for i in range(10001):\n    pass", false, sec=true),
        C("RX-P25", ProgrammingLanguage.PYTHON, "print('x'*70000)", true, "output truncated"),
        C("RX-P26", ProgrammingLanguage.PYTHON, "def f():\n    return f()\nf()", false, err="RecursionError"),
        C("RX-P27", ProgrammingLanguage.PYTHON, "print('한글 실행 성공')", true, "한글 실행 성공"),
        C("RX-P28", ProgrammingLanguage.PYTHON, "d={'a':1,'b':2}\ns=0\nfor k in d:\n    s+=d[k]\nprint(s)", true, "3"),
        C("RX-P29", ProgrammingLanguage.PYTHON, "a=[1,2,3,4]\nprint(a[1:3])", true, "[2, 3]"),
        C("RX-P30", ProgrammingLanguage.PYTHON, "try:\n    x=1/0\nexcept ZeroDivisionError:\n    print('caught')", true, "caught")
    )

    private fun typeScriptCases() = listOf(
        C("RX-T01", ProgrammingLanguage.TYPESCRIPT, "let x:number=2; console.log(x+3);", true, "5"),
        C("RX-T02", ProgrammingLanguage.TYPESCRIPT, "const s:string='hello'; console.log(s.toUpperCase());", true, "HELLO"),
        C("RX-T03", ProgrammingLanguage.TYPESCRIPT, "const a:number[]=[1,2,3]; console.log(a.map(x=>x*2).join(','));", true, "2,4,6"),
        C("RX-T04", ProgrammingLanguage.TYPESCRIPT, "interface S {name:string; price:number}; const s:S={name:'A',price:3}; console.log(s.name,s.price);", true, "A 3"),
        C("RX-T05", ProgrammingLanguage.TYPESCRIPT, "function add(a:number,b:number):number{return a+b}; console.log(add(4,7));", true, "11"),
        C("RX-T06", ProgrammingLanguage.TYPESCRIPT, "function f(x:string|number){console.log(typeof x==='string'?x.length:x+1)}; f('abc');", true, "3"),
        C("RX-T07", ProgrammingLanguage.TYPESCRIPT, "interface U {name:string; age?:number}; const u:U={name:'K'}; console.log(u.age ?? 'none');", true, "none"),
        C("RX-T08", ProgrammingLanguage.TYPESCRIPT, "enum G {A,B}; console.log(G.B);", true, "1"),
        C("RX-T09", ProgrammingLanguage.TYPESCRIPT, "class Box { constructor(public v:number){} double(){return this.v*2} }; console.log(new Box(4).double());", true, "8"),
        C("RX-T10", ProgrammingLanguage.TYPESCRIPT, "function id<T>(x:T):T{return x}; console.log(id<string>('ok'));", true, "ok"),
        C("RX-T11", ProgrammingLanguage.TYPESCRIPT, "const t:[string,number]=['A',2]; console.log(t[0],t[1]);", true, "A 2"),
        C("RX-T12", ProgrammingLanguage.TYPESCRIPT, "const o={a:1,b:2}; const {a,b}=o; console.log(a+b);", true, "3"),
        C("RX-T13", ProgrammingLanguage.TYPESCRIPT, "let s=0; for(let i=0;i<5;i++){s+=i}; console.log(s);", true, "10"),
        C("RX-T14", ProgrammingLanguage.TYPESCRIPT, "const x:number=2; switch(x){case 2: console.log('two'); break; default: console.log('other')}", true, "two"),
        C("RX-T15", ProgrammingLanguage.TYPESCRIPT, "type P={x:number}; const p:P={x:9}; console.log(p.x);", true, "9"),
        C("RX-T16", ProgrammingLanguage.TYPESCRIPT, "let x:number = ;", false, err="compile error"),
        C("RX-T17", ProgrammingLanguage.TYPESCRIPT, "let x:number='wrong'; console.log(x);", false, err="not assignable"),
        C("RX-T18", ProgrammingLanguage.TYPESCRIPT, "console.log(doesNotExist);", false, err="Cannot find name"),
        C("RX-T19", ProgrammingLanguage.TYPESCRIPT, "fe" + "tch('https://example.invalid');", false, sec=true),
        C("RX-T20", ProgrammingLanguage.TYPESCRIPT, "new XML" + "HttpRequest();", false, sec=true),
        C("RX-T21", ProgrammingLanguage.TYPESCRIPT, "new Web" + "Socket('wss://example.invalid');", false, sec=true),
        C("RX-T22", ProgrammingLanguage.TYPESCRIPT, "new Wor" + "ker('x.js');", false, sec=true),
        C("RX-T23", ProgrammingLanguage.TYPESCRIPT, "im" + "port('x').then(()=>{});", false, sec=true),
        C("RX-T24", ProgrammingLanguage.TYPESCRIPT, "requ" + "ire('x');", false, sec=true),
        C("RX-T25", ProgrammingLanguage.TYPESCRIPT, "while(true){}", false, timeout=true),
        C("RX-T26", ProgrammingLanguage.TYPESCRIPT, "throw new Error('boom');", false, err="boom"),
        C("RX-T27", ProgrammingLanguage.TYPESCRIPT, "const s:string='타입스크립트 성공'; console.log(s);", true, "타입스크립트 성공"),
        C("RX-T28", ProgrammingLanguage.TYPESCRIPT, "const n:number=4; console.log(`value=${'$'}{n}`);", true, "value=4"),
        C("RX-T29", ProgrammingLanguage.TYPESCRIPT, "const u:{a?:{b:number}}={}; console.log(u.a?.b ?? 0);", true, "0"),
        C("RX-T30", ProgrammingLanguage.TYPESCRIPT, "let x:string=null; console.log(x);", false, err="not assignable")
    )
}
