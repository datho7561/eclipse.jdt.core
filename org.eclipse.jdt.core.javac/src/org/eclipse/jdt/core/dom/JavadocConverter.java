/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.jdt.core.dom;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.runtime.ILog;

import com.sun.tools.javac.tree.DCTree;
import com.sun.tools.javac.tree.DCTree.DCAuthor;
import com.sun.tools.javac.tree.DCTree.DCBlockTag;
import com.sun.tools.javac.tree.DCTree.DCDeprecated;
import com.sun.tools.javac.tree.DCTree.DCDocComment;
import com.sun.tools.javac.tree.DCTree.DCEndElement;
import com.sun.tools.javac.tree.DCTree.DCEntity;
import com.sun.tools.javac.tree.DCTree.DCIdentifier;
import com.sun.tools.javac.tree.DCTree.DCLink;
import com.sun.tools.javac.tree.DCTree.DCLiteral;
import com.sun.tools.javac.tree.DCTree.DCParam;
import com.sun.tools.javac.tree.DCTree.DCReference;
import com.sun.tools.javac.tree.DCTree.DCReturn;
import com.sun.tools.javac.tree.DCTree.DCSee;
import com.sun.tools.javac.tree.DCTree.DCSince;
import com.sun.tools.javac.tree.DCTree.DCStartElement;
import com.sun.tools.javac.tree.DCTree.DCText;
import com.sun.tools.javac.tree.DCTree.DCThrows;
import com.sun.tools.javac.tree.DCTree.DCUnknownBlockTag;
import com.sun.tools.javac.tree.DCTree.DCUnknownInlineTag;
import com.sun.tools.javac.tree.DCTree.DCUses;

class JavadocConverter {
	
	private final AST ast;
	private final JavacConverter javacConverter;
	private final DCDocComment docComment;
	private final int initialOffset;
	private final int endOffset;

	JavadocConverter(JavacConverter javacConverter, DCDocComment docComment) {
		this.javacConverter = javacConverter;
		this.ast = javacConverter.ast;
		this.docComment = docComment;
		this.initialOffset = this.javacConverter.rawText.substring(0, docComment.getSourcePosition(0)).lastIndexOf("/**");
		int offsetEnd = this.docComment.getSourcePosition(this.docComment.getEndPosition());
		this.endOffset = offsetEnd + this.javacConverter.rawText.substring(offsetEnd).indexOf("*/") + "*/".length();
	}

	private void commonSettings(ASTNode res, DCTree javac) {
		if (javac != null) {
			int length = javac.getEndPosition() - javac.getStartPosition();
			res.setSourceRange(this.docComment.getSourcePosition(javac.getStartPosition()), Math.max(0, length));
		}
		//this.domToJavac.put(res, javac);
	}

	Javadoc convertJavadoc() {
		Javadoc res = this.ast.newJavadoc();
		res.setSourceRange(this.initialOffset, this.endOffset);
		IDocElement[] elements = Stream.of(docComment.preamble, docComment.fullBody, docComment.postamble, docComment.tags)
			.flatMap(List::stream)
			.map(this::convertElement)
			.toArray(IDocElement[]::new);
		TagElement host = null;
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] instanceof TagElement tag && !isInline(tag)) {
				if (host != null) {
					res.tags().add(host);
					host = null;
				}
				res.tags().add(tag);
			} else {
				if (host == null) {
					host = this.ast.newTagElement();
				}
				host.fragments().add(elements[i]);
			}
		}
		if (host != null) {
			res.tags().add(host);
		}
		return res;
	}

	private boolean isInline(TagElement tag) {
		return tag.getTagName() != null && switch (tag.getTagName()) {
			case TagElement.TAG_CODE,
				TagElement.TAG_DOCROOT,
				TagElement.TAG_INHERITDOC,
				TagElement.TAG_LINK,
				TagElement.TAG_LINKPLAIN,
				TagElement.TAG_LITERAL,
				TagElement.TAG_SNIPPET,
				TagElement.TAG_VALUE -> true;
			default -> false;
		};
	}

	private Optional<TagElement> convertBlockTag(DCTree javac) {
		TagElement res = this.ast.newTagElement();
		commonSettings(res, javac);
		if (javac instanceof DCAuthor author) {
			res.setTagName(TagElement.TAG_AUTHOR);
			author.name.stream().map(this::convertElement).forEach(res.fragments::add);
		} else if (javac instanceof DCSince since) {
			res.setTagName(TagElement.TAG_SINCE);
			since.body.stream().map(this::convertElement).forEach(res.fragments::add);
		}  else if (javac instanceof DCSee see) {
			res.setTagName(TagElement.TAG_SEE);
			see.reference.stream().map(this::convertElement).forEach(res.fragments::add);
		} else if (javac instanceof DCDeprecated deprecated) {
			res.setTagName(TagElement.TAG_DEPRECATED);
			deprecated.body.stream().map(this::convertElement).forEach(res.fragments::add);
		} else if (javac instanceof DCParam param) {
			res.setTagName(TagElement.TAG_PARAM);
			res.fragments().add(convertElement(param.name));
			param.description.stream().map(this::convertElement).forEach(res.fragments::add);
		} else if (javac instanceof DCReturn ret) {
			res.setTagName(TagElement.TAG_RETURN);
			ret.description.stream().map(this::convertElement).forEach(res.fragments::add);
		} else if (javac instanceof DCThrows thrown) {
			res.setTagName(TagElement.TAG_THROWS);
			res.fragments().add(convertElement(thrown.name));
			thrown.description.stream().map(this::convertElement).forEach(res.fragments::add);
		} else if (javac instanceof DCUses uses) {
			res.setTagName(TagElement.TAG_USES);
			res.fragments().add(convertElement(uses.serviceType));
			uses.description.stream().map(this::convertElement).forEach(res.fragments::add);
		} else if (javac instanceof DCUnknownBlockTag unknown) {
			res.setTagName(unknown.getTagName());
			unknown.content.stream().map(this::convertElement).forEach(res.fragments::add);
		} else {
			return Optional.empty();
		}
		return Optional.of(res);
	}

	private Optional<TagElement> convertInlineTag(DCTree javac) {
		TagElement res = this.ast.newTagElement();
		commonSettings(res, javac);
		if (javac instanceof DCLiteral literal) {
			res.setTagName(switch (literal.getKind()) {
				case CODE -> TagElement.TAG_CODE;
				case LITERAL -> TagElement.TAG_LITERAL;
				default -> TagElement.TAG_LITERAL;
			});
			res.fragments().add(convertElement(literal.body));
		} else if (javac instanceof DCLink link) {
			res.setTagName(TagElement.TAG_LINK);
			res.fragments().add(convertElement(link.ref));
			link.label.stream().map(this::convertElement).forEach(res.fragments()::add);
		} else if (javac instanceof DCUnknownInlineTag unknown) {
			res.fragments().add(toDefaultTextElement(unknown));
		} else {
			return Optional.empty();
		}
		return Optional.of(res);
	}
	private IDocElement convertElement(DCTree javac) {
		if (javac instanceof DCText text) {
			JavaDocTextElement res = this.ast.newJavaDocTextElement();
			commonSettings(res, javac);
			res.setText(text.getBody());
			return res;
		} else if (javac instanceof DCIdentifier identifier) {
			Name res = this.ast.newName(identifier.getName().toString());
			commonSettings(res, javac);
			return res;
		} else if (javac instanceof DCReference reference) {
			String signature = reference.getSignature();
			if (signature.charAt(signature.length() - 1) == ')') {
				MethodRef res = this.ast.newMethodRef();
				commonSettings(res, javac);
				if (reference.memberName != null) {
					SimpleName name = this.ast.newSimpleName(reference.memberName.toString());
					// TODO set range
					res.setName(name);
				}
				if (reference.qualifierExpression != null) {
					res.setQualifier(this.javacConverter.toName(reference.qualifierExpression));
				}
				// TODO here: fix 
//				reference.paramTypes.stream().map(this.javacConverter::toName).forEach(res.parameters()::add);
				return res;
			} else {
				MemberRef res = this.ast.newMemberRef();
				commonSettings(res, javac);
				if (reference.memberName != null) {
					SimpleName name = this.ast.newSimpleName(reference.memberName.toString());
					// TODO set range
					res.setName(name);
				}
				res.setQualifier(this.javacConverter.toName(reference.qualifierExpression));
				return res;
			}
		} else if (javac instanceof DCStartElement || javac instanceof DCEndElement || javac instanceof DCEntity) {
			return toDefaultTextElement(javac);
		} else if (javac instanceof DCBlockTag || javac instanceof DCReturn) {
			Optional<TagElement> blockTag = convertBlockTag(javac);
			if (blockTag.isPresent()) {
				return blockTag.get();
			}
		} else {
			Optional<TagElement> inlineTag = convertInlineTag(javac);
			if (inlineTag.isPresent()) {
				return inlineTag.get();
			}
		}
		var message = "💥🐛 Not supported yet conversion of " + javac.getClass().getSimpleName() + " to element";
		ILog.get().error(message);
		JavaDocTextElement res = this.ast.newJavaDocTextElement();
		commonSettings(res, javac);
		res.setText(this.docComment.comment.getText().substring(javac.getStartPosition(), javac.getEndPosition()) + System.lineSeparator() + message);
		return res;
	}

	private JavaDocTextElement toDefaultTextElement(DCTree javac) {
		JavaDocTextElement res = this.ast.newJavaDocTextElement();
		commonSettings(res, javac);
		res.setText(this.docComment.comment.getText().substring(javac.getStartPosition(), javac.getEndPosition()));
		return res;
	}
}