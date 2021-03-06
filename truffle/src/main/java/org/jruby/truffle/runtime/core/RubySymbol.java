/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.methods.SymbolProcNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;
import org.jruby.util.ByteList;
import org.jruby.util.ByteListHolder;
import org.jruby.util.CodeRangeable;
import org.jruby.util.StringSupport;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the Ruby {@code Symbol} class.
 */
public class RubySymbol extends RubyBasicObject implements CodeRangeable {

    private final String symbol;
    private final ByteList bytes;
    private int codeRange = StringSupport.CR_UNKNOWN;

    private RubySymbol(RubyClass symbolClass, String symbol, ByteList bytes) {
        super(symbolClass);
        this.symbol = symbol;
        this.bytes = bytes;
    }

    public static RubySymbol newSymbol(RubyContext runtime, String name) {
        return runtime.getSymbolTable().getSymbol(name);
    }

    public RubyProc toProc(SourceSection sourceSection, final RubyNode currentNode) {
        // TODO(CS): cache this?

        RubyNode.notDesignedForCompilation();

        final RubyContext context = getContext();

        final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, null, symbol, true, null, false);

        final RubyRootNode rootNode = new RubyRootNode(context, sourceSection, new FrameDescriptor(), sharedMethodInfo,
                new SymbolProcNode(context, sourceSection, symbol));

        final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);

        return new RubyProc(context.getCoreLibrary().getProcClass(), RubyProc.Type.PROC, sharedMethodInfo, callTarget,
                callTarget, callTarget, null, null, getContext().getCoreLibrary().getNilObject(), null);
    }

    public ByteList getSymbolBytes() {
        return bytes;
    }

    public org.jruby.RubySymbol getJRubySymbol() {
        RubyNode.notDesignedForCompilation();

        return getContext().getRuntime().newSymbol(bytes);
    }

    @Override
    public int hashCode() {
        return symbol.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (other instanceof RubySymbol) {
            return symbol == ((RubySymbol) other).symbol;
        } else if (other instanceof RubyString) {
            return other.equals(symbol);
        } else {
            return super.equals(other);
        }
    }

    @Override
    public String toString() {
        return symbol;
    }

    public RubyString toRubyString() {
         return getContext().makeString(toString());
    }

    @Override
    public int getCodeRange() {
        return codeRange;
    }

    @Override
    @CompilerDirectives.TruffleBoundary
    public int scanForCodeRange() {
        int cr = getCodeRange();

        if (cr == StringSupport.CR_UNKNOWN) {
            cr = slowCodeRangeScan();
            setCodeRange(cr);
        }

        return cr;
    }

    @Override
    public boolean isCodeRangeValid() {
        return codeRange == StringSupport.CR_VALID;
    }

    @Override
    public final void setCodeRange(int codeRange) {
        this.codeRange = codeRange;
    }

    @Override
    public final void clearCodeRange() {
        codeRange = StringSupport.CR_UNKNOWN;
    }

    @Override
    public final void modify() {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void modify(int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Encoding checkEncoding(ByteListHolder other) {
        // TODO (nirvdrum Jan. 13, 2015): This should check if the encodings are compatible rather than just always succeeding.
        return bytes.getEncoding();
    }

    @Override
    public ByteList getByteList() {
        return bytes;
    }

    @CompilerDirectives.TruffleBoundary
    private int slowCodeRangeScan() {
        return StringSupport.codeRangeScan(bytes.getEncoding(), bytes);
    }

    public static final class SymbolTable {

        private final ConcurrentHashMap<ByteList, RubySymbol> symbolsTable = new ConcurrentHashMap<>();
        private final RubyContext context;

        public SymbolTable(RubyContext context) {
            this.context = context;
        }

        public RubySymbol getSymbol(String name) {
            ByteList byteList = org.jruby.RubySymbol.symbolBytesFromString(context.getRuntime(), name);

            RubySymbol symbol = symbolsTable.get(byteList);

            if (symbol == null) {
                symbol = createSymbol(name);
            }
            return symbol;
        }

        public RubySymbol getSymbol(ByteList byteList) {
            // TODO(CS): is this broken? ByteList is mutable...

            RubySymbol symbol = symbolsTable.get(byteList);

            if (symbol == null) {
                symbol = createSymbol(byteList);
            }
            return symbol;

        }

        private RubySymbol createSymbol(ByteList byteList) {
            RubySymbol symbol = new RubySymbol(context.getCoreLibrary().getSymbolClass(), byteList.toString(), byteList);
            symbolsTable.put(byteList, symbol);
            return symbol;
        }

        private RubySymbol createSymbol(String name) {
            ByteList byteList = org.jruby.RubySymbol.symbolBytesFromString(context.getRuntime(), name);
            RubySymbol symbol = new RubySymbol(context.getCoreLibrary().getSymbolClass(), name, byteList);

            RubySymbol existingSymbol = symbolsTable.putIfAbsent(byteList, symbol);
            return existingSymbol == null ? symbol : existingSymbol;
        }

        public ConcurrentHashMap<ByteList, RubySymbol> getSymbolsTable(){
            return symbolsTable;
        }
    }

    @Override
    public boolean hasNoSingleton() {
        return true;
    }

}
