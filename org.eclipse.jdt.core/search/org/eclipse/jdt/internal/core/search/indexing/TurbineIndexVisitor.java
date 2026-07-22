package org.eclipse.jdt.internal.core.search.indexing;

import com.google.turbine.tree.Tree;
import com.google.turbine.tree.Tree.*;
import com.google.turbine.tree.TurbineModifier;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.Modifier;

public class TurbineIndexVisitor implements Tree.Visitor<Void, Void> {

	private SourceIndexer sourceIndexer;

	private char[] packageName;
	private List<TyDecl> enclosingTypes = new LinkedList<>();

	public TurbineIndexVisitor(SourceIndexer sourceIndexer) {
		this.sourceIndexer = sourceIndexer;
	}

	@Override
	public Void visitIdent(Ident ident, Void input) {
		// TODO
		return null;
	}

	@Override
	public Void visitWildTy(WildTy visitor, Void input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitArrTy(ArrTy arrTy, Void input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitPrimTy(PrimTy primTy, Void input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitVoidTy(VoidTy primTy, Void input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitClassTy(ClassTy visitor, Void input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitLiteral(Literal literal, Void input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitParen(Paren unary, Void input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitTypeCast(TypeCast typeCast, Void input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitUnary(Unary unary, Void input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitBinary(Binary binary, Void input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitConstVarName(ConstVarName constVarName, Void input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitClassLiteral(ClassLiteral classLiteral, Void input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitAssign(Assign assign, Void input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitConditional(Conditional conditional, Void input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitArrayInit(ArrayInit arrayInit, Void input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitCompUnit(CompUnit compUnit, Void input) {
		compUnit.pkg().ifPresent(pkg -> pkg.accept(this, null));
		for (var decl : compUnit.decls()) {
			decl.accept(this, null);
		}
		return null;
	}

	@Override
	public Void visitImportDecl(ImportDecl importDecl, Void input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitVarDecl(VarDecl varDecl, Void input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitMethDecl(MethDecl methDecl, Void input) {
		char[] methodName = methDecl.name().value().toCharArray();
		char[][] parameterTypes = methDecl.params().stream()
			.map(myVar -> myVar.ty())
			.filter(Type.class::isInstance)
			.map(Type.class::cast)
			.map(Type::toString)
			.map(String::toCharArray)
			.toArray(char[][]::new);
		char[] returnType = methDecl.ret().isEmpty() ? null : methDecl.ret().get().toString().toCharArray();
//		char[][] exceptionTypes = ((List<Type>)method.thrownExceptionTypes()).stream()
//			.map(Type::toString)
//			.map(String::toCharArray)
//			.toArray(char[][]::new);
//		char[][] parameterNames = ((List<VariableDeclaration>)method.parameters()).stream()
//				.map(VariableDeclaration::getName)
//				.map(SimpleName::getIdentifier)
//				.map(String::toCharArray)
//				.toArray(char[][]::new);
//		if (!method.isConstructor()) {
//			this.sourceIndexer.addMethodDeclaration(methodName, parameterTypes, returnType, exceptionTypes);
//			if (!this.enclosingTypes.isEmpty()) {
//				this.sourceIndexer.addMethodDeclaration(this.enclosingTypes.get(this.enclosingTypes.size() - 1).getName().getIdentifier().toCharArray(),
//					null /* TODO: fully qualified name of enclosing type? */,
//					methodName,
//					parameterTypes.length,
//					null,
//					parameterTypes,
//					parameterNames,
//					returnType,
//					method.getModifiers() | maybeDeprecated(method),
//					this.packageName,
//					0 /* TODO What to put here? */,
//					exceptionTypes,
//					0 /* TODO ExtraFlags.IsLocalType ? */);
//			}
//		} else {
//			this.sourceIndexer.addConstructorDeclaration(method.getName().getFullyQualifiedName().toCharArray(),
//					method.parameters().size(),
//					null, parameterTypes, parameterNames, method.getModifiers() | maybeDeprecated(method), this.packageName, currentType().getModifiers(), exceptionTypes, 0);
//		}
		return null;
	}

	@Override
	public Void visitAnno(Anno anno, Void input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitTyDecl(TyDecl tyDecl, Void input) {
		char[][] enclosing =
			this.enclosingTypes.stream()
			.map(TyDecl::name)
			.map(Ident::value)
			.map(String::toCharArray)
			.toArray(char[][]::new);

		char[][] parameterTypeSignatures = tyDecl.typarams().stream()
				.map(TyParam::name)
				.map(Ident::value)
				.map(name -> Signature.createTypeSignature(name, false))
				.map(String::toCharArray)
				.toArray(char[][]::new);
		if (tyDecl.mods().contains(TurbineModifier.INTERFACE)) {
			this.sourceIndexer.addInterfaceDeclaration(turbineModsToJDTMods(tyDecl.mods()) | deprecatedAnnotations(tyDecl.annos()),
					this.packageName,
					tyDecl.name().value().toCharArray(),
					enclosing,
					tyDecl.impls().stream().map(ClassTy::name).map(Ident::value).map(String::toCharArray).toArray(char[][]::new),
					parameterTypeSignatures,
					isSecondary(tyDecl));
		} else {
			this.sourceIndexer.addClassDeclaration(
					turbineModsToJDTMods(tyDecl.mods()) | deprecatedAnnotations(tyDecl.annos()),
					this.packageName,
					tyDecl.name().value().toCharArray(),
					enclosing,
					tyDecl.xtnds().isPresent() ? tyDecl.xtnds().get().name().value().toCharArray() : null,
					tyDecl.impls().stream().map(ClassTy::name).map(Ident::value).map(String::toCharArray).toArray(char[][]::new),
					parameterTypeSignatures,
					isSecondary(tyDecl));
			if (tyDecl.members().stream().noneMatch(member -> member instanceof MethDecl method && method.name().value().equals("<init>"))) { //$NON-NLS-1$
				this.sourceIndexer.addDefaultConstructorDeclaration(
						tyDecl.name().value().toCharArray(),
						this.packageName,
						turbineModsToJDTMods(tyDecl.mods()) | deprecatedAnnotations(tyDecl.annos()),
						0);
			}
			if (tyDecl.xtnds().isPresent()) {
				this.sourceIndexer.addConstructorReference(tyDecl.xtnds().get().name().value().toCharArray(), 0);
			}
		}

		// children
		this.enclosingTypes.add(tyDecl);
		for (var member : tyDecl.members()) {
			member.accept(this, null);
		}
		this.enclosingTypes.remove(this.enclosingTypes.size() - 1);

		return null;
	}

	private boolean isSecondary(TyDecl tyDecl) {
		return this.enclosingTypes.isEmpty() &&
			!Objects.equals(tyDecl.name().value() + ".java", Path.of(this.sourceIndexer.document.getPath()).getFileName().toString()); //$NON-NLS-1$
	}

	private static int deprecatedAnnotations(List<Anno> annos) {
		boolean containsIt = annos.stream().anyMatch(anno -> {
			return anno.name().get(0).toString().equals("Deprecated"); //$NON-NLS-1$
		});
		if (containsIt) {
			return org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.AccDeprecated;
		}
		return 0;
	}

	private static int turbineModsToJDTMods(Set<TurbineModifier> mods) {
		int jdtMods = 0;
		for (TurbineModifier mod : mods) {
			switch (mod) {
				case PUBLIC: {
					jdtMods |= Modifier.PUBLIC;
					break;
				}
				case PROTECTED: {
					jdtMods |= Modifier.PROTECTED;
					break;
				}
				case PRIVATE: {
					jdtMods |= Modifier.PRIVATE;
					break;
				}
				case STATIC: {
					jdtMods |= Modifier.STATIC;
					break;
				}
				case ABSTRACT: {
					jdtMods |= Modifier.ABSTRACT;
					break;
				}
				case FINAL: {
					jdtMods |= Modifier.FINAL;
					break;
				}
				case NATIVE: {
					jdtMods |= Modifier.NATIVE;
					break;
				}
				case SYNCHRONIZED: {
					jdtMods |= Modifier.SYNCHRONIZED;
					break;
				}
				case TRANSIENT: {
					jdtMods |= Modifier.TRANSIENT;
					break;
				}
				case VOLATILE: {
					jdtMods |= Modifier.VOLATILE;
					break;
				}
				case STRICTFP: {
					jdtMods |= Modifier.STRICTFP;
					break;
				}
				case DEFAULT: {
					jdtMods |= Modifier.DEFAULT;
					break;
				}
				case SEALED: {
					jdtMods |= Modifier.SEALED;
					break;
				}
				case NON_SEALED: {
					jdtMods |= Modifier.NON_SEALED;
					break;
				}
				default:
					// do nothing
			}
		}
		return jdtMods;
	}

	@Override
	public Void visitTyParam(TyParam tyParam, Void input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitPkgDecl(PkgDecl pkgDecl, Void input) {
		this.packageName = pkgDecl.name().stream().map(Ident::value).collect(Collectors.joining(".")).toCharArray(); //$NON-NLS-1$
		return null;
	}

	@Override
	public Void visitModDecl(ModDecl modDecl, Void input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitModRequires(ModRequires modRequires, Void input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitModExports(ModExports modExports, Void input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitModOpens(ModOpens modOpens, Void input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitModUses(ModUses modUses, Void input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitModProvides(ModProvides modProvides, Void input) {
		// TODO Auto-generated method stub
		return null;
	}

}
