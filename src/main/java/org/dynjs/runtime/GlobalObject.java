package org.dynjs.runtime;

import org.dynjs.Config;
import org.dynjs.compiler.JSCompiler;
import org.dynjs.runtime.PropertyDescriptor.Names;
import org.dynjs.runtime.builtins.*;
import org.dynjs.runtime.builtins.Math;
import org.dynjs.runtime.builtins.types.*;
import org.dynjs.runtime.java.JavaPackage;

import java.util.ArrayList;
import java.util.List;

public class GlobalObject extends DynObject {

    private DynJS runtime;
    private BlockManager blockManager;
    private List<AbstractBuiltinType> builtinTypes = new ArrayList<>();

    public GlobalObject(DynJS runtime) {
        super(null);
        this.runtime = runtime;
        this.blockManager = new BlockManager();

        defineReadOnlyGlobalProperty("__throwTypeError", new ThrowTypeError(this));

        // ----------------------------------------
        // Built-in types
        // ----------------------------------------

        registerBuiltinType("Object", new BuiltinObject(this));
        registerBuiltinType("Function", new BuiltinFunction(this));
        registerBuiltinType("Boolean", new BuiltinBoolean(this));
        registerBuiltinType("Number", new BuiltinNumber(this));
        registerBuiltinType("Array", new BuiltinArray(this));
        registerBuiltinType("String", new BuiltinString(this));
        registerBuiltinType("RegExp", new BuiltinRegExp(this));
        registerBuiltinType("Date", new BuiltinDate(this));
        registerBuiltinType("Error", new BuiltinError(this));
        registerBuiltinType("ReferenceError", new BuiltinReferenceError(this));
        registerBuiltinType("RangeError", new BuiltinRangeError(this));
        registerBuiltinType("SyntaxError", new BuiltinSyntaxError(this));
        registerBuiltinType("TypeError", new BuiltinTypeError(this));
        registerBuiltinType("URIError", new BuiltinURIError(this));
        registerBuiltinType("EvalError", new BuiltinEvalError(this));

        initializeBuiltinTypes();

        // ----------------------------------------
        // Built-in global functions
        // ----------------------------------------

        defineReadOnlyGlobalProperty("undefined", Types.UNDEFINED);

        defineGlobalProperty("parseFloat", new ParseFloat(this));
        defineGlobalProperty("parseInt", new ParseInt(this));
        defineGlobalProperty("eval", new Eval(this));
        defineGlobalProperty("isNaN", new IsNaN(this));
        defineGlobalProperty("isFinite", new IsFinite(this));

        defineGlobalProperty("encodeURI", new EncodeUri(this));
        defineGlobalProperty("decodeURI", new DecodeUri(this));
        defineGlobalProperty("encodeURIComponent", new EncodeUriComponent(this));
        defineGlobalProperty("decodeURIComponent", new DecodeUriComponent(this));

        if (runtime.getConfig().isNodePackageManagerEnabled()) {
            defineGlobalProperty("require", new Require(this));
        }
        defineGlobalProperty("include", new Include(this));
        defineGlobalProperty("load", new Include(this)); // hackety hack
        defineGlobalProperty("escape", new Escape(this));
        defineGlobalProperty("unescape", new Unescape(this));
        defineGlobalProperty("print", new Print(this));
        defineGlobalProperty("dynjs", new DynJSBuiltin(this.runtime));

        // ----------------------------------------
        // Built-in global objects
        // ----------------------------------------

        put(null, "JSON", new JSON(this), false);
        defineGlobalProperty("Math", new Math(this));
        defineGlobalProperty("Intl", new Intl(this));

        // ----------------------------------------
        // Java integration
        // ----------------------------------------

        defineGlobalProperty("Packages", new JavaPackage(this, null));
        defineGlobalProperty("java",     new JavaPackage(this, "java"));
        defineGlobalProperty("org",      new JavaPackage(this, "org"));
        defineGlobalProperty("com",      new JavaPackage(this, "com"));
        defineGlobalProperty("io",       new JavaPackage(this, "io"));

        setPrototype(getPrototypeFor("Object"));

    }

    private void registerBuiltinType(String name, final AbstractBuiltinType type) {
        PropertyDescriptor desc = new PropertyDescriptor();
        desc.set(Names.VALUE, type);
        desc.set(Names.ENUMERABLE, false);
        desc.set(Names.WRITABLE, true);
        desc.set(Names.CONFIGURABLE, true);
        defineOwnProperty(null, name, desc, false);
        put(null, "__Builtin_" + name, type, false);
        this.builtinTypes.add(type);
    }

    private void initializeBuiltinTypes() {
        for (AbstractBuiltinType each : this.builtinTypes) {
            each.setPrototype(getPrototypeFor("Function"));
            each.initialize(this);
        }
    }

    public static GlobalObject newGlobalObject(DynJS runtime) {
        return runtime.getConfig().getGlobalObjectFactory().newGlobalObject(runtime);
    }

    public DynJS getRuntime() {
        return this.runtime;
    }

    public Config getConfig() {
        return getRuntime().getConfig();
    }

    public JSCompiler getCompiler() {
        return this.runtime.getCompiler();
    }

    public BlockManager getBlockManager() {
        return this.blockManager;
    }

    public org.dynjs.runtime.BlockManager.Entry retrieveBlockEntry(int statementNumber) {
        return this.blockManager.retrieve(statementNumber);
    }

    public void defineGlobalProperty(final String name, final Object value) {
        PropertyDescriptor desc = new PropertyDescriptor();
        desc.set(Names.VALUE, value);
        desc.set(Names.WRITABLE, true);
        desc.set(Names.ENUMERABLE, false);
        desc.set(Names.CONFIGURABLE, true);
        defineOwnProperty(null, name, desc, false);
    }

    public void defineReadOnlyGlobalProperty(final String name, final Object value) {
        PropertyDescriptor desc = new PropertyDescriptor();
        desc.set(Names.VALUE, value);
        desc.set(Names.WRITABLE, false);
        desc.set(Names.CONFIGURABLE, false);
        desc.set(Names.ENUMERABLE, false);
        defineOwnProperty(null, name, desc, false);
    }

    public JSObject getPrototypeFor(String type) {
        Object typeObj = get(null, type);
        if (typeObj == Types.UNDEFINED) {
            return null;
        }
        Object prototype = ((JSObject) typeObj).get(null, "prototype");
        if (prototype == Types.UNDEFINED) {
            return null;
        }
        return (JSObject) prototype;
    }

}
