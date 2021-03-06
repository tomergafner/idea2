/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.psi.formatter.xml;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageFormatting;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.formatter.FormatterUtil;
import com.intellij.psi.xml.XmlElementType;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class XmlTagBlock extends AbstractXmlBlock{
  private final Indent myIndent;

  public XmlTagBlock(final ASTNode node,
                     final Wrap wrap,
                     final Alignment alignment,
                     final XmlFormattingPolicy policy,
                     final Indent indent) {
    super(node, wrap, alignment, policy);
    myIndent = indent;
  }

  protected List<Block> buildChildren() {
    ASTNode child = myNode.getFirstChildNode();
    final Wrap attrWrap = Wrap.createWrap(getWrapType(myXmlFormattingPolicy.getAttributesWrap()), false);
    final Wrap textWrap = Wrap.createWrap(getWrapType(myXmlFormattingPolicy.getTextWrap(getTag())), true);
    final Wrap tagBeginWrap = createTagBeginWrapping(getTag());
    final Alignment attrAlignment = Alignment.createAlignment();
    final Alignment textAlignment = Alignment.createAlignment();
    final ArrayList<Block> result = new ArrayList<Block>(3);
    ArrayList<Block> localResult = new ArrayList<Block>(1);

    boolean insideTag = true;

    while (child != null) {
      if (!FormatterUtil.containsWhiteSpacesOnly(child) && child.getTextLength() > 0){

        Wrap wrap = chooseWrap(child, tagBeginWrap, attrWrap, textWrap);
        Alignment alignment = chooseAlignment(child, attrAlignment, textAlignment);

        if (child.getElementType() == XmlElementType.XML_TAG_END) {
          child = processChild(localResult,child, wrap, alignment, null);
          result.add(createTagDescriptionNode(localResult));
          localResult = new ArrayList<Block>(1);
          insideTag = true;
        }
        else if (child.getElementType() == XmlElementType.XML_START_TAG_START) {
          insideTag = false;
          if (!localResult.isEmpty()) {
            result.add(createTagContentNode(localResult));
          }
          localResult = new ArrayList<Block>(1);
          child = processChild(localResult,child, wrap, alignment, null);
        }
        else if (child.getElementType() == XmlElementType.XML_END_TAG_START) {
          insideTag = false;
          if (!localResult.isEmpty()) {
            result.add(createTagContentNode(localResult));
            localResult = new ArrayList<Block>(1);
          }
          child = processChild(localResult,child, wrap, alignment, null);
        } else if (child.getElementType() == XmlElementType.XML_EMPTY_ELEMENT_END) {
          child = processChild(localResult,child, wrap, alignment, null);
          result.add(createTagDescriptionNode(localResult));
          localResult = new ArrayList<Block>(1);
        }
        else if (isJspxJavaContainingNode(child)) {
          createJspTextNode(localResult, child, getChildIndent());
        }
        /*
        else if (child.getElementType() == ElementType.XML_TEXT) {
          child  = createXmlTextBlocks(localResult, child, wrap, alignment);
        }
        */
        else {
          final Indent indent;

          if (isJspResult(localResult)) {
            //indent = FormatterEx.getInstance().getNoneIndent();
            indent = getChildrenIndent();
          } else if (!insideTag) {
            indent = null;
          }
          else {
            indent = getChildrenIndent();
          }

          child = processChild(localResult,child, wrap, alignment, indent);
        }
      }
      if (child != null) {
        child = child.getTreeNext();
      }
    }

    if (!localResult.isEmpty()) {
      result.add(createTagContentNode(localResult));
    }

    return result;

  }

  protected boolean isJspResult(final ArrayList<Block> localResult) {
    return false;
  }

  protected
  @Nullable
  ASTNode processChild(List<Block> result, final ASTNode child, final Wrap wrap, final Alignment alignment, final Indent indent) {
    IElementType type = child.getElementType();
    if (type == XmlElementType.XML_TEXT) {
      final PsiElement parent = child.getPsi().getParent();

      if (parent instanceof XmlTag && ((XmlTag)parent).getSubTags().length == 0) {
        if (buildInjectedPsiBlocks(result, child, wrap, alignment, indent)) return child;
      }
      return createXmlTextBlocks(result, child, wrap, alignment);
    } else if (type == XmlElementType.XML_COMMENT) {
      if (buildInjectedPsiBlocks(result, child, wrap, alignment, indent)) return child;
      return super.processChild(result, child, wrap, alignment, indent);
    }
    else {
      return super.processChild(result, child, wrap, alignment, indent);
    }
  }

  private boolean buildInjectedPsiBlocks(List<Block> result, final ASTNode child, Wrap wrap, Alignment alignment, Indent indent) {
    final PsiFile[] injectedFile = new PsiFile[1];
    final Ref<Integer> offset = new Ref<Integer>();
    final Ref<Integer> offset2 = new Ref<Integer>();
    final Ref<Integer> prefixLength = new Ref<Integer>();
    final Ref<Integer> suffixLength = new Ref<Integer>();

    ((PsiLanguageInjectionHost)child.getPsi()).processInjectedPsi(new PsiLanguageInjectionHost.InjectedPsiVisitor() {
      public void visit(@NotNull final PsiFile injectedPsi, @NotNull final List<PsiLanguageInjectionHost.Shred> places) {
        if (places.size() == 1) {
          final PsiLanguageInjectionHost.Shred shred = places.get(0);
          final TextRange textRange = shred.getRangeInsideHost();
          String childText;

          if (( child.getTextLength() == textRange.getEndOffset() &&
                textRange.getStartOffset() == 0
              ) ||
              ( canProcessFragments((childText = child.getText()).substring(0, textRange.getStartOffset())) &&
                canProcessFragments(childText.substring(textRange.getEndOffset()))
              )
             ) {
            injectedFile[0] = injectedPsi;
            offset.set(textRange.getStartOffset());
            offset2.set(textRange.getEndOffset());
            prefixLength.set(shred.prefix != null ? shred.prefix.length():0);
            suffixLength.set(shred.suffix != null ? shred.suffix.length():0);
          }
        }
      }

      private boolean canProcessFragments(String s) {
        IElementType type = child.getElementType();
        if (type == XmlElementType.XML_TEXT) {
          s = s.trim();
          s = s.replace("<![CDATA[","");
          s = s.replace("]]>","");
        } else if (type == XmlElementType.XML_COMMENT) {   // <!--[if IE]>, <![endif]--> of conditional comments injection
          s = "";
        }

        return s.length() == 0;
      }
    });

    if  (injectedFile[0] != null) {
      final Language childLanguage = injectedFile[0].getLanguage();
      final FormattingModelBuilder builder = LanguageFormatting.INSTANCE.forContext(childLanguage, child.getPsi());

      if (builder != null) {
        final int startOffset = offset.get().intValue();
        final int endOffset = offset2.get().intValue();
        TextRange range = child.getTextRange();

        int childOffset = range.getStartOffset();
        if (startOffset != 0) {
          final ASTNode leaf = child.findLeafElementAt(startOffset - 1);
          result.add(new XmlBlock(leaf, wrap, alignment, myXmlFormattingPolicy, indent, new TextRange(childOffset, childOffset + startOffset)));
        }

        createAnotherLanguageBlockWrapper(childLanguage, injectedFile[0].getNode(), result, indent,
                                          childOffset + startOffset,
                                          new TextRange(prefixLength.get(), injectedFile[0].getTextLength() - suffixLength.get()));

        if (endOffset != child.getTextLength()) {
          final ASTNode leaf = child.findLeafElementAt(endOffset);
          result.add(new XmlBlock(leaf, wrap, alignment, myXmlFormattingPolicy, indent, new TextRange(childOffset + endOffset, range.getEndOffset())));
        }
        return true;
      }
    }
    return false;
  }

  private Indent getChildrenIndent() {
    return myXmlFormattingPolicy.indentChildrenOf(getTag())
           ? Indent.getNormalIndent()
           : Indent.getNoneIndent();
  }

  public Indent getIndent() {
    return myIndent;
  }

  private ASTNode createXmlTextBlocks(final List<Block> list, final ASTNode textNode, final Wrap wrap, final Alignment alignment) {
    ASTNode child = textNode.getFirstChildNode();
    return createXmlTextBlocks(list, textNode, child, wrap, alignment);
  }

  private ASTNode createXmlTextBlocks(final List<Block> list, final ASTNode textNode, ASTNode child,
                                      final Wrap wrap,
                                      final Alignment alignment
  ) {
    while (child != null) {
      if (!FormatterUtil.containsWhiteSpacesOnly(child) && child.getTextLength() > 0){
        final Indent indent = getChildrenIndent();
        child = processChild(list,child,  wrap, alignment, indent);
        if (child == null) return child;
        if (child.getTreeParent() != textNode) {
          if (child.getTreeParent() != myNode) {
            return createXmlTextBlocks(list, child.getTreeParent(), child.getTreeNext(), wrap, alignment);
          } else {
            return child;
          }
        }
      }
      child = child.getTreeNext();
    }
    return textNode;
  }

  private Block createTagContentNode(final ArrayList<Block> localResult) {
    return createSyntheticBlock(localResult, getChildrenIndent());
  }

  protected Block createSyntheticBlock(final ArrayList<Block> localResult, final Indent childrenIndent) {
    return new SyntheticBlock(localResult, this, Indent.getNoneIndent(), myXmlFormattingPolicy, childrenIndent);
  }

  private Block createTagDescriptionNode(final ArrayList<Block> localResult) {
    return createSyntheticBlock(localResult, null);
  }

  public Spacing getSpacing(Block child1, Block child2) {
    final AbstractSyntheticBlock syntheticBlock1 = ((AbstractSyntheticBlock)child1);
    final AbstractSyntheticBlock syntheticBlock2 = ((AbstractSyntheticBlock)child2);

    if (syntheticBlock2.startsWithCDATA() || syntheticBlock1.endsWithCDATA()) {
      return Spacing.getReadOnlySpacing();
    }

    if (syntheticBlock2.isJspTextBlock() || syntheticBlock1.isJspTextBlock()) {
      return Spacing.createSafeSpacing(myXmlFormattingPolicy.getShouldKeepLineBreaks(), myXmlFormattingPolicy.getKeepBlankLines());
    }

    if (syntheticBlock2.isJspxTextBlock() || syntheticBlock1.isJspxTextBlock()) {
      return Spacing.createSpacing(0, 0, 1, myXmlFormattingPolicy.getShouldKeepLineBreaks(), myXmlFormattingPolicy.getKeepBlankLines());
    }

    if (myXmlFormattingPolicy.keepWhiteSpacesInsideTag(getTag())) return Spacing.getReadOnlySpacing();

    if (myXmlFormattingPolicy.getShouldKeepWhiteSpaces()) {
      return Spacing.getReadOnlySpacing();
    }

    if (syntheticBlock2.startsWithTag() ) {
      final XmlTag startTag = syntheticBlock2.getStartTag();
      if (myXmlFormattingPolicy.keepWhiteSpacesInsideTag(startTag) && startTag.textContains('\n')) {
        return getChildrenIndent() != Indent.getNoneIndent() ? Spacing.getReadOnlySpacing():Spacing.createSpacing(0,0,0,true,myXmlFormattingPolicy.getKeepBlankLines());
      }
    }

    boolean saveSpacesBetweenTagAndText = myXmlFormattingPolicy.shouldSaveSpacesBetweenTagAndText() &&
      syntheticBlock1.getTextRange().getEndOffset() < syntheticBlock2.getTextRange().getStartOffset();

    if (syntheticBlock1.endsWithTextElement() && syntheticBlock2.startsWithTextElement()) {
      return Spacing.createSafeSpacing(myXmlFormattingPolicy.getShouldKeepLineBreaksInText(), myXmlFormattingPolicy.getKeepBlankLines());
    }

    if (syntheticBlock1.endsWithText()) { //text</tag
      if (syntheticBlock1.insertLineFeedAfter()) {
        return Spacing.createDependentLFSpacing(0, 0, getTag().getTextRange(), myXmlFormattingPolicy.getShouldKeepLineBreaks(), myXmlFormattingPolicy.getKeepBlankLines());
      }
      if (saveSpacesBetweenTagAndText) {
        return Spacing.createSafeSpacing(myXmlFormattingPolicy.getShouldKeepLineBreaks(), myXmlFormattingPolicy.getKeepBlankLines());
      }
      return Spacing.createSpacing(0, 0, 0, myXmlFormattingPolicy.getShouldKeepLineBreaks(), myXmlFormattingPolicy.getKeepBlankLines());

    } else if (syntheticBlock1.isTagDescription() && syntheticBlock2.isTagDescription()) { //></
      return Spacing.createSpacing(0, 0, 0, myXmlFormattingPolicy.getShouldKeepLineBreaks(), myXmlFormattingPolicy.getKeepBlankLines());
    } else if (syntheticBlock2.startsWithText()) { //>text
      if (saveSpacesBetweenTagAndText) {
        return Spacing.createSafeSpacing(true, myXmlFormattingPolicy.getKeepBlankLines());
      }
      return Spacing.createSpacing(0, 0, 0, true, myXmlFormattingPolicy.getKeepBlankLines());
    } else if (syntheticBlock1.isTagDescription() && syntheticBlock2.startsWithTag()) {
      return Spacing.createSpacing(0, 0, 0, true, myXmlFormattingPolicy.getKeepBlankLines());
    } else if (syntheticBlock1.insertLineFeedAfter()) {
      return Spacing.createSpacing(0,0,1,true,myXmlFormattingPolicy.getKeepBlankLines());
    } else if (syntheticBlock1.endsWithTag() && syntheticBlock2.isTagDescription()) {
      return Spacing.createSpacing(0, 0, 0, true, myXmlFormattingPolicy.getKeepBlankLines());
    } else {
      return createDefaultSpace(true, true);
    }

  }

  public boolean insertLineBreakBeforeTag() {
    return myXmlFormattingPolicy.insertLineBreakBeforeTag(getTag());
  }

  public boolean removeLineBreakBeforeTag() {
    return myXmlFormattingPolicy.removeLineBreakBeforeTag(getTag());
  }

  public boolean isTextElement() {
    return myXmlFormattingPolicy.isTextElement(getTag());
  }

  @Override
  @NotNull
  public ChildAttributes getChildAttributes(final int newChildIndex) {
    if (isAfterAttribute(newChildIndex)) {
      List<Block> subBlocks = getSubBlocks();
      Block subBlock = subBlocks.get(newChildIndex - 1);
      int prevSubBlockChildrenCount = subBlock.getSubBlocks().size();
      return subBlock.getChildAttributes(prevSubBlockChildrenCount);
    }
    else {
      if (myXmlFormattingPolicy.indentChildrenOf(getTag())) {
        return new ChildAttributes(Indent.getNormalIndent(), null);
      } else {
        return new ChildAttributes(Indent.getNoneIndent(), null);
      }
    }
  }

  private boolean isAfterAttribute(final int newChildIndex) {
    List<Block> subBlocks = getSubBlocks();
    Block prevBlock = subBlocks.get(newChildIndex - 1);
    return prevBlock instanceof SyntheticBlock && ((SyntheticBlock)prevBlock).endsWithAttribute();
  }
}
