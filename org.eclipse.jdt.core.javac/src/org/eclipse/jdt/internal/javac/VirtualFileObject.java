package org.eclipse.jdt.internal.javac;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;

import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;

public class VirtualFileObject implements JavaFileObject {
	private ICompilationUnit sourceUnit;
	private Kind kind;
	public VirtualFileObject(ICompilationUnit sourceUnit, Kind kind) {
		this.sourceUnit = sourceUnit;
		this.kind = kind;
	}

	@Override
	public URI toUri() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		return new String(sourceUnit.getFileName());
	}

	@Override
	public InputStream openInputStream() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OutputStream openOutputStream() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append(this.sourceUnit.getContents());
		return sb;
	}

	@Override
	public Writer openWriter() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getLastModified() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean delete() {
		return false;
	}

	@Override
	public Kind getKind() {
		return this.kind;
	}

	@Override
	public boolean isNameCompatible(String simpleName, Kind kind) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public NestingKind getNestingKind() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Modifier getAccessLevel() {
		// TODO Auto-generated method stub
		return null;
	}

}
