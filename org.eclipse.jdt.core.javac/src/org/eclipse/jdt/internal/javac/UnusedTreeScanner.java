/*******************************************************************************
* Copyright (c) 2024 Microsoft Corporation and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License 2.0
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Microsoft Corporation - initial API and implementation
*******************************************************************************/

package org.eclipse.jdt.internal.javac;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jdt.core.compiler.CategorizedProblem;

import com.sun.source.doctree.SeeTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type.JCPrimitiveType;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.parser.Tokens.Comment.CommentStyle;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.JCTree.JCMemberReference;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

public class UnusedTreeScanner<R, P> extends TreeScanner<R, P> {
	final Set<Tree> privateDecls = new LinkedHashSet<>();
	final Set<Symbol> usedElements = new HashSet<>();
	final Map<String, JCImport> unusedImports = new LinkedHashMap<>();
	private CompilationUnitTree unit = null;
	
	private final UnusedDocTreeScanner unusedDocTreeScanner = new UnusedDocTreeScanner();
	
	@Override
	public R scan(Tree tree, P p) {
		if (tree == null) {
			return super.scan(tree, p);
		}
		JCCompilationUnit jcUnit = null;
		if (unit instanceof JCCompilationUnit currentUnit) {
			jcUnit = currentUnit;
		} else if (tree instanceof JCCompilationUnit currentUnit) {
			jcUnit = currentUnit;
		}
		
		if (jcUnit != null && tree instanceof JCTree jcTree) {
			Comment c = jcUnit.docComments.getComment(jcTree);
			if (c != null && (c.getStyle() == CommentStyle.JAVADOC_BLOCK || c.getStyle() == CommentStyle.JAVADOC_LINE)) {
				var docCommentTree = jcUnit.docComments.getCommentTree(jcTree);
				this.unusedDocTreeScanner.scan(docCommentTree, p);
			}
		}
		return super.scan(tree, p);
	}

	@Override
	public R visitCompilationUnit(CompilationUnitTree node, P p) {
		this.unit = node;
		return super.visitCompilationUnit(node, p);
	}

	@Override
	public R visitImport(ImportTree node, P p) {
		if (node instanceof JCImport jcImport) {
			String importClass = jcImport.qualid.toString();
			this.unusedImports.put(importClass, jcImport);
		}

		return super.visitImport(node, p);
	}

	@Override
	public R visitClass(ClassTree node, P p) {
		if (node instanceof JCClassDecl classDecl && this.isPotentialUnusedDeclaration(classDecl)) {
			this.privateDecls.add(classDecl);
		}

		return super.visitClass(node, p);
	}

	@Override
	public R visitIdentifier(IdentifierTree node, P p) {
		if (node instanceof JCIdent id && isPrivateSymbol(id.sym)) {
			this.usedElements.add(id.sym);
		}

		if (node instanceof JCIdent id && isMemberSymbol(id.sym)) {
			String name = id.toString();
			String ownerName = id.sym.owner.toString();
			if (!ownerName.isBlank()) {
				String starImport = ownerName + ".*";
				String usualImport = ownerName + "." + name;
				if (this.unusedImports.containsKey(starImport)) {
					this.unusedImports.remove(starImport);
				} else if (this.unusedImports.containsKey(usualImport)) {
					this.unusedImports.remove(usualImport);
				}
			}
		}

		return super.visitIdentifier(node, p);
	}

	@Override
	public R visitMemberSelect(MemberSelectTree node, P p) {
		if (node instanceof JCFieldAccess field) {
			if (isPrivateSymbol(field.sym)) {
				this.usedElements.add(field.sym);
			}
		}

		return super.visitMemberSelect(node, p);
	}

	@Override
	public R visitMethod(MethodTree node, P p) {
		boolean isPrivateMethod = this.isPotentialUnusedDeclaration(node);
		if (isPrivateMethod) {
			this.privateDecls.add(node);
		}

		return super.visitMethod(node, p);
	}

	@Override
	public R visitVariable(VariableTree node, P p) {
		boolean isPrivateVariable = this.isPotentialUnusedDeclaration(node);
		if (isPrivateVariable) {
			this.privateDecls.add(node);
		}

		return super.visitVariable(node, p);
	}

	@Override
	public R visitMemberReference(MemberReferenceTree node, P p) {
		if (node instanceof JCMemberReference member && isPrivateSymbol(member.sym)) {
			this.usedElements.add(member.sym);
		}

		return super.visitMemberReference(node, p);
	}

	@Override
	public R visitNewClass(NewClassTree node, P p) {
		if (node instanceof JCNewClass newClass) {
			Symbol targetClass = newClass.def != null ? newClass.def.sym : newClass.type.tsym;
			if (isPrivateSymbol(targetClass)) {
				this.usedElements.add(targetClass);
			}
		}

		return super.visitNewClass(node, p);
	}

	private boolean isPotentialUnusedDeclaration(Tree tree) {
		if (tree instanceof JCClassDecl classTree) {
			return (classTree.getModifiers().flags & Flags.PRIVATE) != 0;
		} else if (tree instanceof JCMethodDecl methodTree) {
			if (isConstructor(methodTree)) {
				return (methodTree.getModifiers().flags & Flags.PRIVATE) != 0
						&& hasPackageVisibleConstructor(methodTree.sym.owner);
			}
			return (methodTree.getModifiers().flags & Flags.PRIVATE) != 0;
		} else if (tree instanceof JCVariableDecl variable) {
			Symbol owner = variable.sym == null ? null : variable.sym.owner;
			if (owner instanceof ClassSymbol) {
				return !isSerialVersionConstant(variable) && (variable.getModifiers().flags & Flags.PRIVATE) != 0;
			} else if (owner instanceof MethodSymbol) {
				return true;
			}
		}

		return false;
	}

	private boolean isConstructor(JCMethodDecl methodDecl) {
		return methodDecl.sym != null
				&& methodDecl.sym.isConstructor();
	}

	private boolean hasPackageVisibleConstructor(Symbol symbol) {
		if (symbol instanceof ClassSymbol clazz) {
			for (var member : clazz.members().getSymbols()) {
				if (member instanceof MethodSymbol method) {
					if (method.isConstructor() && (method.flags() & Flags.PRIVATE) == 0) {
						return true;
					}
				}
			}
		}

		return false;
	}

	private boolean isPrivateSymbol(Symbol symbol) {
		if (symbol instanceof ClassSymbol
				|| symbol instanceof MethodSymbol) {
			return (symbol.flags() & Flags.PRIVATE) != 0;
		} else if (symbol instanceof VarSymbol) {
			if (symbol.owner instanceof ClassSymbol) {
				return (symbol.flags() & Flags.PRIVATE) != 0;
			} else if (symbol.owner instanceof MethodSymbol) {
				return true;
			}
		}

		return false;
	}

	private boolean isMemberSymbol(Symbol symbol) {
		if (symbol instanceof ClassSymbol
				|| symbol instanceof MethodSymbol) {
			return true;
		}

		if (symbol instanceof VarSymbol) {
			return symbol.owner instanceof ClassSymbol;
		}

		return false;
	}

	private boolean isSerialVersionConstant(JCVariableDecl variable) {
		long flags = variable.getModifiers().flags;
		return (flags & Flags.FINAL) != 0
				&& (flags & Flags.STATIC) != 0
				&& variable.type instanceof JCPrimitiveType type
				&& type.getTag() == TypeTag.LONG
				&& "serialVersionUID".equals(variable.name.toString());
	}

	public List<CategorizedProblem> getUnusedImports(UnusedProblemFactory problemFactory) {
		return problemFactory.addUnusedImports(this.unit, this.unusedImports);
	}

	public List<CategorizedProblem> getUnusedPrivateMembers(UnusedProblemFactory problemFactory) {
		List<Tree> unusedPrivateMembers = new ArrayList<>();
		for (Tree decl : this.privateDecls) {
			if (decl instanceof JCClassDecl classDecl && !this.usedElements.contains(classDecl.sym)) {
				unusedPrivateMembers.add(decl);	
			} else if (decl instanceof JCMethodDecl methodDecl && !this.usedElements.contains(methodDecl.sym)) {
				unusedPrivateMembers.add(decl);
			} else if (decl instanceof JCVariableDecl variableDecl && !this.usedElements.contains(variableDecl.sym)) {
				unusedPrivateMembers.add(decl);
			}
		}

		return problemFactory.addUnusedPrivateMembers(unit, unusedPrivateMembers);
	}
	
	private class UnusedDocTreeScanner extends com.sun.source.util.DocTreeScanner<R, P> {
		@Override
		public R visitLink(com.sun.source.doctree.LinkTree node, P p) {
			if (node.getReference() instanceof com.sun.tools.javac.tree.DCTree.DCReference ref) {
				useImport(ref);
			}
			return super.visitLink(node, p);
		}

		@Override
		public R visitSee(SeeTree node, P p) {
			if (node.getReference() instanceof List<?> refs) {
				for (Object ref : refs) {
					if (ref instanceof com.sun.tools.javac.tree.DCTree.DCReference) {
						useImport((com.sun.tools.javac.tree.DCTree.DCReference)ref);
					}
				}
			}
			return super.visitSee(node, p);
		}

		private void useImport(com.sun.tools.javac.tree.DCTree.DCReference ref) {
			if (ref.qualifierExpression instanceof JCIdent qualifier) {
				String fieldName = null;
				// for static imports
				if (ref.memberName instanceof JCIdent field) {
					fieldName = field.toString();
				}

				if (qualifier.sym == null || qualifier.sym.owner.toString().isBlank()) {
					String suffix = "." + qualifier.getName().toString();
					Optional<String> potentialImport = UnusedTreeScanner.this.unusedImports.keySet().stream().filter(a -> a.endsWith(suffix)).findFirst();
					if (potentialImport.isPresent()) {
						UnusedTreeScanner.this.unusedImports.remove(potentialImport.get());
					}
					// static imports
					if (fieldName != null) {
						String suffixWithField = suffix + "." + fieldName;
						String suffixWithWildcard = suffix + ".*";
						Optional<String> potentialStaticImport = UnusedTreeScanner.this.unusedImports.keySet().stream().filter(a -> a.endsWith(suffixWithField)).findFirst();
						if (potentialStaticImport.isPresent()) {
							UnusedTreeScanner.this.unusedImports.remove(potentialStaticImport.get());
						}
						Optional<String> potentialStaticWildcardImport = UnusedTreeScanner.this.unusedImports.keySet().stream().filter(a -> a.endsWith(suffixWithWildcard)).findFirst();
						if (potentialStaticWildcardImport.isPresent()) {
							UnusedTreeScanner.this.unusedImports.remove(potentialStaticWildcardImport.get());
						}
					}
				} else {
					String name = qualifier.toString();
					String ownerName = qualifier.sym.owner.toString();
					if (!ownerName.isBlank()) {
						String starImport = ownerName + ".*";
						String usualImport = ownerName + "." + name;
						if (UnusedTreeScanner.this.unusedImports.containsKey(starImport)) {
							UnusedTreeScanner.this.unusedImports.remove(starImport);
						} else if (UnusedTreeScanner.this.unusedImports.containsKey(usualImport)) {
							UnusedTreeScanner.this.unusedImports.remove(usualImport);
						}
						// static imports
						if (fieldName != null) {
							String suffixWithField = usualImport + "." + fieldName;
							String suffixWithWildcard = usualImport + ".*";
							if (UnusedTreeScanner.this.unusedImports.containsKey(suffixWithField)) {
								UnusedTreeScanner.this.unusedImports.remove(suffixWithField);
							}
							if (UnusedTreeScanner.this.unusedImports.containsKey(suffixWithWildcard)) {
								UnusedTreeScanner.this.unusedImports.remove(suffixWithWildcard);
							}
						}
					}
				}
			}
		}
	}
}
