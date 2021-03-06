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

package com.intellij.lang.impl;

import com.intellij.lang.*;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.pom.PomManager;
import com.intellij.pom.PomModel;
import com.intellij.pom.event.PomModelEvent;
import com.intellij.pom.impl.PomTransactionBase;
import com.intellij.pom.tree.TreeAspect;
import com.intellij.pom.tree.TreeAspectEvent;
import com.intellij.pom.tree.events.TreeChangeEvent;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.TokenType;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.impl.source.text.ASTDiffBuilder;
import com.intellij.psi.impl.source.tree.*;
import com.intellij.psi.text.BlockSupport;
import com.intellij.psi.tree.*;
import com.intellij.util.CharTable;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ThreeState;
import com.intellij.util.containers.Convertor;
import com.intellij.util.containers.LimitedPool;
import com.intellij.util.containers.Stack;
import com.intellij.util.diff.DiffTree;
import com.intellij.util.diff.DiffTreeChangeBuilder;
import com.intellij.util.diff.FlyweightCapableTreeStructure;
import com.intellij.util.diff.ShallowNodeComparator;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: max
 * Date: Jan 21, 2005
 * Time: 3:30:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class PsiBuilderImpl extends UserDataHolderBase implements PsiBuilder {
  private static final Logger LOG = Logger.getInstance("#com.intellij.lang.impl.PsiBuilderImpl");

  private int[] myLexStarts;
  private IElementType[] myLexTypes;

  private final MyList myProduction = new MyList();

  private final Lexer myLexer;
  private final boolean myFileLevelParsing;
  private final TokenSet myWhitespaces;
  private TokenSet myComments;

  private CharTable myCharTable;
  private int myCurrentLexem;
  private final CharSequence myText;
  private final char[] myTextArray;
  private boolean myDebugMode = false;
  private ASTNode myOriginalTree = null;
  private int myLexemCount = 0;
  boolean myTokenTypeChecked;

  private static TokenSet ourAnyLanguageWhitespaceTokens = TokenSet.EMPTY;

  private final LimitedPool<StartMarker> START_MARKERS = new LimitedPool<StartMarker>(2000, new LimitedPool.ObjectFactory<StartMarker>() {
    public StartMarker create() {
      return new StartMarker();
    }

    public void cleanup(final StartMarker startMarker) {
      startMarker.clean();
    }
  });

  private final LimitedPool<DoneMarker> DONE_MARKERS = new LimitedPool<DoneMarker>(2000, new LimitedPool.ObjectFactory<DoneMarker>() {
    public DoneMarker create() {
      return new DoneMarker();
    }

    public void cleanup(final DoneMarker doneMarker) {
      doneMarker.clean();
    }
  });

  @NonNls private static final String UNBALANCED_MESSAGE =
    "Unbalanced tree. Most probably caused by unbalanced markers. Try calling setDebugMode(true) against PsiBuilder passed to identify exact location of the problem";
  private PsiElement myInjectionHost;
  private ITokenTypeRemapper myRemapper;

  public static void registerWhitespaceToken(IElementType type) {
    ourAnyLanguageWhitespaceTokens = TokenSet.orSet(ourAnyLanguageWhitespaceTokens, TokenSet.create(type));
  }

  public PsiBuilderImpl(Language lang, Lexer lexer, final ASTNode chameleon, Project project, CharSequence text) {
    myText = text;
    myTextArray = CharArrayUtil.fromSequenceWithoutCopying(text);
    ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(lang);
    assert parserDefinition != null : "ParserDefinition absent for language: " + lang.getID();
    myLexer = lexer != null ? lexer : parserDefinition.createLexer(project);
    myWhitespaces = parserDefinition.getWhitespaceTokens();
    myComments = parserDefinition.getCommentTokens();
    myCharTable = SharedImplUtil.findCharTableByTree(chameleon);

    myOriginalTree = chameleon.getUserData(BlockSupport.TREE_TO_BE_REPARSED);
    myInjectionHost = chameleon.getPsi().getContext();

    myFileLevelParsing = myCharTable == null || myOriginalTree != null;
    cacheLexems();
  }

  @TestOnly
  public PsiBuilderImpl(final Lexer lexer, final TokenSet whitespaces, final TokenSet comments, CharSequence text) {
    myWhitespaces = whitespaces;
    myLexer = lexer;
    myComments = comments;
    myText = text;
    myTextArray = CharArrayUtil.fromSequenceWithoutCopying(text);

    myFileLevelParsing = true;
    cacheLexems();
  }

  @TestOnly
  public void setOriginalTree(final ASTNode originalTree) {
    myOriginalTree = originalTree;
    myCharTable = SharedImplUtil.findCharTableByTree(originalTree);
  }

  private void cacheLexems() {
    int approxLexCount = Math.max(10, myText.length() / 5);

    myLexStarts = new int[approxLexCount];
    myLexTypes = new IElementType[approxLexCount];

    int i = 0;

    myLexer.start(myText);
    while (true) {
      IElementType type = myLexer.getTokenType();
      if (type == null) break;

      if (i >= myLexTypes.length - 1) {
        resizeLexems(i * 3 / 2);
      }
      myLexStarts[i] = myLexer.getTokenStart();
      myLexTypes[i] = type;
      i++;
      myLexer.advance();
    }

    myLexStarts[i] = myText.length();

    myLexemCount = i;
  }

  public void enforceCommentTokens(TokenSet tokens) {
    myComments = tokens;
  }

  @Nullable
  public PsiElement getInjectionHost() {
    return myInjectionHost;
  }

  private static abstract class Node implements LighterASTNode {
    public abstract int hc();
  }

  private static class StartMarker extends ProductionMarker implements Marker {
    public PsiBuilderImpl myBuilder;
    public IElementType myType;
    public DoneMarker myDoneMarker;
    public Throwable myDebugAllocationPosition;
    public ProductionMarker firstChild;
    public ProductionMarker lastChild;
    private int myHC = -1;

    public void clean() {
      super.clean();
      myBuilder = null;
      myType = null;
      myDoneMarker = null;
      myDebugAllocationPosition = null;
      firstChild = null;
      lastChild = null;
      myHC = -1;
    }

    public int hc() {
      if (myHC == -1) {
        PsiBuilderImpl builder = myBuilder;
        int hc = 0;
        final CharSequence buf = builder.myText;
        final char[] bufArray = builder.myTextArray;
        ProductionMarker child = firstChild;
        int lexIdx = myLexemIndex;

        while (child != null) {
          int lastLeaf = child.myLexemIndex;
          for (int i = builder.myLexStarts[lexIdx]; i < builder.myLexStarts[lastLeaf]; i++) {
            hc += bufArray != null ? bufArray[i] : buf.charAt(i);
          }
          lexIdx = lastLeaf;
          hc += child.hc();
          if (child instanceof StartMarker) {
            lexIdx = ((StartMarker)child).myDoneMarker.myLexemIndex;
          }
          child = child.next;
        }

        for (int i = builder.myLexStarts[lexIdx]; i < builder.myLexStarts[myDoneMarker.myLexemIndex]; i++) {
          hc += bufArray != null ? bufArray[i]:buf.charAt(i);
        }

        myHC = hc;
      }

      return myHC;
    }

    public int getStartOffset() {
      return myBuilder.myLexStarts[myLexemIndex];
    }

    public int getEndOffset() {
      return myBuilder.myLexStarts[myDoneMarker.myLexemIndex];
    }

    public void addChild(ProductionMarker node) {
      if (firstChild == null) {
        firstChild = node;
        lastChild = node;
      }
      else {
        lastChild.next = node;
        lastChild = node;
      }
    }

    public Marker precede() {
      return myBuilder.precede(this);
    }

    public void drop() {
      myBuilder.drop(this);
    }

    public void rollbackTo() {
      myBuilder.rollbackTo(this);
    }

    public void done(IElementType type) {
      myType = type;
      myBuilder.done(this);
    }

    public void doneBefore(IElementType type, Marker before) {
      myType = type;
      myBuilder.doneBefore(this, before);
    }

    public void doneBefore(final IElementType type, final Marker before, final String errorMessage) {
      myBuilder.myProduction.add(myBuilder.myProduction.lastIndexOf(before), new ErrorItem(myBuilder, errorMessage, ((StartMarker)before).myLexemIndex));
      doneBefore(type, before);
    }

    public void error(String message) {
      myType = TokenType.ERROR_ELEMENT;
      myBuilder.error(this, message);
    }

    public IElementType getTokenType() {
      return myType;
    }
  }

  private Marker precede(final StartMarker marker) {
    int idx = myProduction.lastIndexOf(marker);
    if (idx < 0) {
      LOG.error("Cannot precede dropped or rolled-back marker");
    }
    StartMarker pre = createMarker(marker.myLexemIndex);
    myProduction.add(idx, pre);
    return pre;
  }

  private class Token extends Node {
    public IElementType myTokenType;
    public int myTokenStart;
    public int myTokenEnd;
    public int myHC = -1;

    public int hc() {
      if (myHC == -1) {
        int hc = 0;
        if (myTokenType instanceof TokenWrapper){
          final String value = ((TokenWrapper)myTokenType).getValue();
          for (int i = 0; i < value.length(); i++) {
            hc += value.charAt(i);
          }
        }
        else {
          final int start = myTokenStart;
          final int end = myTokenEnd;
          final CharSequence buf = myText;
          final char[] bufArray = myTextArray;

          for (int i = start; i < end; i++) {
            hc += bufArray != null ? bufArray[i] : buf.charAt(i);
          }
        }

        myHC = hc;
      }

      return myHC;
    }

    public int getEndOffset() {
      return myTokenEnd;
    }

    public int getStartOffset() {
      return myTokenStart;
    }

    public CharSequence getText() {
      if (myTokenType instanceof TokenWrapper) {
        return ((TokenWrapper)myTokenType).getValue();
      }

      return myText.subSequence(myTokenStart, myTokenEnd);
    }

    public IElementType getTokenType() {
      return myTokenType;
    }
  }

  private abstract static class ProductionMarker extends Node {
    public int myLexemIndex;
    ProductionMarker next;

    public void clean() {
      myLexemIndex = 0;
      next = null;
    }
  }

  private static class DoneMarker extends ProductionMarker {
    public StartMarker myStart;

    public DoneMarker() {}

    public DoneMarker(final StartMarker marker, int currentLexem) {
      myLexemIndex = currentLexem;
      myStart = marker;
    }

    public int hc() {
      throw new UnsupportedOperationException("Shall not be called on this kind of markers");
    }

    public IElementType getTokenType() {
      throw new UnsupportedOperationException("Shall not be called on this kind of markers");
    }

    public int getEndOffset() {
      throw new UnsupportedOperationException("Shall not be called on this kind of markers");
    }

    public int getStartOffset() {
      throw new UnsupportedOperationException("Shall not be called on this kind of markers");
    }

    public void clean() {
      super.clean();
      myStart = null;
    }
  }

  private static class DoneWithErrorMarker extends DoneMarker {
    public String myMessage;

    public DoneWithErrorMarker(final StartMarker marker, int currentLexem, String message) {
      super(marker, currentLexem);
      myMessage = message;
    }

    public void clean() {
      super.clean();
      myMessage = null;
    }
  }

  private static class ErrorItem extends ProductionMarker {
    String myMessage;
    private final PsiBuilderImpl myBuilder;

    public ErrorItem(PsiBuilderImpl builder, final String message, int idx) {
      myBuilder = builder;
      myLexemIndex = idx;
      myMessage = message;
    }

    public int hc() {
      return 0;
    }

    public int getEndOffset() {
      return myBuilder.myLexStarts[myLexemIndex];
    }

    public int getStartOffset() {
      return myBuilder.myLexStarts[myLexemIndex];
    }

    public IElementType getTokenType() {
      return TokenType.ERROR_ELEMENT;
    }

    public void clean() {
      super.clean();
      myMessage = null;
    }
  }

  public CharSequence getOriginalText() {
    return myText;
  }

  public IElementType getTokenType() {
    if (eof()) return null;

    if (myRemapper != null) {
      IElementType type = myLexTypes[myCurrentLexem];
      type = myRemapper.filter(type, myLexStarts[myCurrentLexem], myLexStarts[myCurrentLexem + 1], myLexer.getBufferSequence());
      myLexTypes[myCurrentLexem] = type; // filter may have changed the type 
      return type;
    }
    return myLexTypes[myCurrentLexem];
  }

  public void setTokenTypeRemapper(final ITokenTypeRemapper remapper) {
    myRemapper = remapper;
  }

  public void advanceLexer() {
    if (!myTokenTypeChecked) {
      LOG.assertTrue(eof(), "Probably a bug: eating token without its type checking");
    }
    myTokenTypeChecked = false;
    myCurrentLexem++;
  }

  private void skipWhitespace() {
    while (myCurrentLexem < myLexemCount && whitespaceOrComment(myLexTypes[myCurrentLexem])) myCurrentLexem++;
  }

  public int getCurrentOffset() {
    if (eof()) return getOriginalText().length();
    return myLexStarts[myCurrentLexem];
  }

  @Nullable
  public String getTokenText() {
    if (eof()) return null;
    final IElementType type = getTokenType();
    if (type instanceof TokenWrapper) {
      return ((TokenWrapper)type).getValue();
    }
    return myText.subSequence(myLexStarts[myCurrentLexem], myLexStarts[myCurrentLexem + 1]).toString();
  }

  private void resizeLexems(final int newSize) {
    int count = Math.min(newSize, myLexTypes.length);
    int[] newStarts = new int[newSize + 1];
    System.arraycopy(myLexStarts, 0, newStarts, 0, count);
    myLexStarts = newStarts;

    IElementType[] newTypes = new IElementType[newSize];
    System.arraycopy(myLexTypes, 0, newTypes, 0, count);
    myLexTypes = newTypes;
  }

  private boolean whitespaceOrComment(IElementType token) {
    return myWhitespaces.contains(token) || myComments.contains(token);
  }

  public Marker mark() {
    if (!myProduction.isEmpty()) {
      skipWhitespace();
    }
    StartMarker marker = createMarker(myCurrentLexem);

    myProduction.add(marker);
    return marker;
  }

  private StartMarker createMarker(final int lexemIndex) {
    StartMarker marker;
    marker = START_MARKERS.alloc();
    marker.myLexemIndex = lexemIndex;
    marker.myBuilder = this;

    if (myDebugMode) {
      marker.myDebugAllocationPosition = new Throwable("Created at the following trace.");
    }
    return marker;
  }

  public final boolean eof() {
    markTokenTypeChecked();
    skipWhitespace();
    return myCurrentLexem >= myLexemCount;
  }

  private void markTokenTypeChecked() {
    myTokenTypeChecked = true;
  }

  @SuppressWarnings({"SuspiciousMethodCalls"})
  private void rollbackTo(Marker marker) {
    myCurrentLexem = ((StartMarker)marker).myLexemIndex;
    markTokenTypeChecked();
    int idx = myProduction.lastIndexOf(marker);
    if (idx < 0) {
      LOG.error("The marker must be added before rolled back to.");
    }
    myProduction.removeRange(idx, myProduction.size());
    START_MARKERS.recycle((StartMarker)marker);
  }

  @SuppressWarnings({"SuspiciousMethodCalls"})
  public void doneBefore(Marker marker, Marker before) {
// TODO: there could be not done markers after 'marker' and that's normal
    if (((StartMarker)marker).myDoneMarker != null) {
      LOG.error("Marker already done.");
    }

    int idx = myProduction.lastIndexOf(marker);
    if (idx < 0) {
      LOG.error("Marker never been added.");
    }

    int beforeIndex = myProduction.lastIndexOf(before);

    DoneMarker doneMarker = DONE_MARKERS.alloc();
    doneMarker.myLexemIndex = ((StartMarker)before).myLexemIndex;
    doneMarker.myStart = (StartMarker)marker;

    ((StartMarker)marker).myDoneMarker = doneMarker;
    myProduction.add(beforeIndex, doneMarker);
  }

  @SuppressWarnings({"SuspiciousMethodCalls"})
  public void drop(Marker marker) {
    final boolean removed = myProduction.remove(myProduction.lastIndexOf(marker)) == marker;
    if (!removed) {
      LOG.error("The marker must be added before it is dropped.");
    }
    START_MARKERS.recycle((StartMarker)marker);
  }

  public void error(Marker marker, String message) {
    doValidityChecks(marker);

    DoneWithErrorMarker doneMarker = new DoneWithErrorMarker((StartMarker)marker, myCurrentLexem, message);
    ((StartMarker)marker).myDoneMarker = doneMarker;
    myProduction.add(doneMarker);
  }

  public void done(Marker marker) {
    doValidityChecks(marker);


    DoneMarker doneMarker = DONE_MARKERS.alloc();
    doneMarker.myStart = (StartMarker)marker;
    doneMarker.myLexemIndex = myCurrentLexem;

    ((StartMarker)marker).myDoneMarker = doneMarker;
    myProduction.add(doneMarker);
  }

  @SuppressWarnings({"UseOfSystemOutOrSystemErr", "SuspiciousMethodCalls"})
  private void doValidityChecks(final Marker marker) {
    if (myDebugMode) {
      final DoneMarker doneMarker = ((StartMarker)marker).myDoneMarker;
      if (doneMarker != null) {
        LOG.error("Marker already done.");
      }
      int idx = myProduction.lastIndexOf(marker);
      if (idx < 0) {
        LOG.error("Marker never been added.");
      }

      for (int i = myProduction.size() - 1; i > idx; i--) {
        Object item = myProduction.get(i);
        if (item instanceof Marker) {
          StartMarker otherMarker = (StartMarker)item;
          if (otherMarker.myDoneMarker == null) {
            final Throwable debugAllocOther = otherMarker.myDebugAllocationPosition;
            final Throwable debugAllocThis = ((StartMarker)marker).myDebugAllocationPosition;
            if (debugAllocOther != null) {
              debugAllocThis.printStackTrace(System.err);
              debugAllocOther.printStackTrace(System.err);
            }
            LOG.error("Another not done marker added after this one. Must be done before this.");
          }
        }
      }
    }
  }

  public void error(String messageText) {
    final ProductionMarker lastMarker = myProduction.get(myProduction.size() - 1);
    if (lastMarker instanceof ErrorItem && lastMarker.myLexemIndex == myCurrentLexem) {
      return;
    }
    myProduction.add(new ErrorItem(this, messageText, myCurrentLexem));
  }

  public ASTNode getTreeBuilt() {
    try {
      StartMarker rootMarker = prepareLightTree();

      if (myOriginalTree != null) {
        merge(myOriginalTree, rootMarker);
        throw new BlockSupport.ReparsedSuccessfullyException();
      }
      else {
        final ASTNode rootNode = createRootAST(rootMarker);

        bind((CompositeElement)rootNode, rootMarker);

        return rootNode;
      }
    }
    finally {
      for (ProductionMarker marker : myProduction) {
        if (marker instanceof StartMarker) {
          START_MARKERS.recycle((StartMarker)marker);
        }
        else if (marker instanceof DoneMarker) {
          DONE_MARKERS.recycle((DoneMarker)marker);
        }
      }
    }
  }

  public FlyweightCapableTreeStructure<LighterASTNode> getLightTree() {
    StartMarker rootMarker = prepareLightTree();
    return new MyTreeStructure(rootMarker);
  }

  private ASTNode createRootAST(final StartMarker rootMarker) {
    final ASTNode rootNode;
    if (myFileLevelParsing) {
      rootNode = new FileElement(rootMarker.myType, null);
      myCharTable = ((FileElement)rootNode).getCharTable();
    }
    else {
      rootNode = createComposite(rootMarker);
      rootNode.putUserData(CharTable.CHAR_TABLE_KEY, myCharTable);
    }
    return rootNode;
  }

  private class MyBuilder implements DiffTreeChangeBuilder<ASTNode, LighterASTNode> {
    private final ASTDiffBuilder myDelegate;
    private final ASTConvertor myConvertor;

    public MyBuilder(PsiFileImpl file, LighterASTNode rootNode) {
      myDelegate = new ASTDiffBuilder(file);
      myConvertor = new ASTConvertor((Node)rootNode);
    }

    public void nodeDeleted(final ASTNode oldParent, final ASTNode oldNode) {
      myDelegate.nodeDeleted(oldParent, oldNode);
    }

    public void nodeInserted(final ASTNode oldParent, final LighterASTNode newNode, final int pos) {
      myDelegate.nodeInserted(oldParent, myConvertor.convert((Node)newNode), pos);
    }

    public void nodeReplaced(final ASTNode oldChild, final LighterASTNode newChild) {
      myDelegate.nodeReplaced(oldChild, myConvertor.convert((Node)newChild));
    }

    public TreeChangeEvent getEvent() {
      return myDelegate.getEvent();
    }
  }

  private void merge(final ASTNode oldNode, final StartMarker newNode) {
    final PsiFileImpl file = (PsiFileImpl)oldNode.getPsi().getContainingFile();
    final PomModel model = PomManager.getModel(file.getProject());

    try {
      model.runTransaction(new PomTransactionBase(file, model.getModelAspect(TreeAspect.class)) {
        public PomModelEvent runInner() throws IncorrectOperationException {
          final MyBuilder builder = new MyBuilder(file, newNode);
          DiffTree.diff(new ASTStructure(oldNode), new MyTreeStructure(newNode), new MyComparator(), builder);
          file.subtreeChanged();

          return new TreeAspectEvent(model, builder.getEvent());
        }
      });
    }
    catch (IncorrectOperationException e) {
      LOG.error(e);
    }
    catch (Throwable e) {
      throw new RuntimeException(UNBALANCED_MESSAGE, e);
    }
  }

  private StartMarker prepareLightTree() {
    markTokenTypeChecked();

    final MyList fProduction = myProduction;
    StartMarker rootMarker = (StartMarker)fProduction.get(0);

    for (int i = 1; i < fProduction.size() - 1; i++) {
      ProductionMarker item = fProduction.get(i);

      if (item instanceof StartMarker) {
        IElementType nextTokenType;
        while (item.myLexemIndex < myLexemCount &&
               ( myWhitespaces.contains(nextTokenType = myLexTypes[item.myLexemIndex]) ||
                 myComments.contains(nextTokenType)
               )
              ) {
          item.myLexemIndex++;
        }
      }
      else if (item instanceof DoneMarker || item instanceof ErrorItem) {
        int prevProductionLexIndex = fProduction.get(i - 1).myLexemIndex;
        IElementType prevTokenType;

        while (item.myLexemIndex > prevProductionLexIndex && item.myLexemIndex - 1 < myLexemCount &&
               ( myWhitespaces.contains(prevTokenType = myLexTypes[item.myLexemIndex - 1]) ||
                 myComments.contains(prevTokenType)
               )
              ) {
          item.myLexemIndex--;
        }
      }
    }

    StartMarker curNode = rootMarker;

    int lastErrorIndex = -1;

    Stack<StartMarker> nodes = new Stack<StartMarker>();
    nodes.push(rootMarker);

    for (int i = 1; i < fProduction.size(); i++) {
      ProductionMarker item = fProduction.get(i);

      if (curNode == null) LOG.error("Unexpected end of the production");

      if (item instanceof StartMarker) {
        StartMarker marker = (StartMarker)item;
        curNode.addChild(marker);
        nodes.push(curNode);
        curNode = marker;
      }
      else if (item instanceof DoneMarker) {
        curNode = nodes.pop();
      }
      else if (item instanceof ErrorItem) {
        int curToken = item.myLexemIndex;
        if (curToken == lastErrorIndex) continue;
        lastErrorIndex = curToken;
        curNode.addChild(item);
      }
    }

    final boolean allTokensInserted = myCurrentLexem >= myLexemCount;
    if (!allTokensInserted) {
      LOG.error("Not all of the tokens inserted to the tree, parsed text:\n" + myText);
    }

    if (myLexStarts.length <= myCurrentLexem + 1) {
      resizeLexems(myCurrentLexem + 1);
    }

    myLexStarts[myCurrentLexem] = myText.length(); // $ terminating token.;
    myLexStarts[myCurrentLexem + 1] = 0;
    myLexTypes[myCurrentLexem] = null;

    LOG.assertTrue(curNode == rootMarker, UNBALANCED_MESSAGE);
    return rootMarker;
  }

  private void bind(CompositeElement ast, StartMarker marker) {
    bind(ast, marker, marker.myLexemIndex);
  }

  private int bind(CompositeElement ast, StartMarker marker, int lexIndex) {
    ProductionMarker child = marker.firstChild;
    while (child != null) {
      if (child instanceof StartMarker) {
        final StartMarker childMarker = (StartMarker)child;

        lexIndex = insertLeafs(lexIndex, childMarker.myLexemIndex, ast);

        CompositeElement childNode = createComposite(childMarker);
        ast.rawAddChildren(childNode);
        lexIndex = bind(childNode, childMarker, lexIndex);

        lexIndex = insertLeafs(lexIndex, childMarker.myDoneMarker.myLexemIndex, ast);
      }
      else if (child instanceof ErrorItem) {
        lexIndex = insertLeafs(lexIndex, child.myLexemIndex, ast);
        final PsiErrorElementImpl errorElement = new PsiErrorElementImpl();
        errorElement.setErrorDescription(((ErrorItem)child).myMessage);
        ast.rawAddChildren(errorElement);
      }

      child = child.next;
    }

    return insertLeafs(lexIndex, marker.myDoneMarker.myLexemIndex, ast);
  }

  private int insertLeafs(int curToken, int lastIdx, final CompositeElement curNode) {
    lastIdx = Math.min(lastIdx, myLexemCount);
    while (curToken < lastIdx) {
      final int start = myLexStarts[curToken];
      final int end = myLexStarts[curToken + 1];
      if (start < end || myLexTypes[curToken] instanceof ILeafElementType) { // Empty token. Most probably a parser directive like indent/dedent in phyton
        final IElementType type = myLexTypes[curToken];
        final TreeElement leaf = createLeaf(type, start, end);
        curNode.rawAddChildren(leaf);
      }
      curToken++;
    }

    return curToken;
  }

  private static CompositeElement createComposite(final StartMarker marker) {
    final IElementType type = marker.myType;
    if (type == TokenType.ERROR_ELEMENT) {
      CompositeElement childNode = new PsiErrorElementImpl();
      if (marker.myDoneMarker instanceof DoneWithErrorMarker) {
        ((PsiErrorElementImpl)childNode).setErrorDescription(((DoneWithErrorMarker)marker.myDoneMarker).myMessage);
      }
      return childNode;
    }

    if (type == null) {
      throw new RuntimeException(UNBALANCED_MESSAGE);
    }

    return ASTFactory.composite(type);
  }

  @Nullable
  public String getErrorMessage(LighterASTNode node) {
    if (node instanceof ErrorItem) return ((ErrorItem)node).myMessage;
    if (node instanceof StartMarker) {
      final StartMarker marker = (StartMarker)node;
      if (marker.myType == TokenType.ERROR_ELEMENT && marker.myDoneMarker instanceof DoneWithErrorMarker) {
        return ((DoneWithErrorMarker)marker.myDoneMarker).myMessage;
      }
    }

    return null;
  }

  private class MyComparator implements ShallowNodeComparator<ASTNode, LighterASTNode> {
    public ThreeState deepEqual(final ASTNode oldNode, final LighterASTNode newNode) {
      if (newNode instanceof Token) {
        if (oldNode instanceof ForeignLeafPsiElement) {
          final IElementType type = newNode.getTokenType();
          if (type instanceof ForeignLeafType) {
            return ((ForeignLeafType)type).getValue().equals(oldNode.getText()) ? ThreeState.YES : ThreeState.NO;
          }
          return ThreeState.NO;
        }

        if (oldNode instanceof LeafElement) {
          return ((LeafElement)oldNode).textMatches(myText, ((Token)newNode).myTokenStart, ((Token)newNode).myTokenEnd)
                 ? ThreeState.YES
                 : ThreeState.NO;
        }

        if (oldNode.getElementType() instanceof ILazyParseableElementType && newNode.getTokenType() instanceof ILazyParseableElementType ||
            oldNode.getElementType() instanceof CustomParsingType && newNode.getTokenType() instanceof CustomParsingType) {
          return ((TreeElement)oldNode).textMatches(myText, ((Token)newNode).myTokenStart, ((Token)newNode).myTokenEnd)
                 ? ThreeState.YES
                 : ThreeState.NO;
        }
      }

      return ThreeState.UNSURE;
    }

    public boolean typesEqual(final ASTNode n1, final LighterASTNode n2) {
      if (n1 instanceof PsiWhiteSpaceImpl) {
        return ourAnyLanguageWhitespaceTokens.contains(n2.getTokenType()) || myWhitespaces.contains(n2.getTokenType());
      }

      return derefToken(n1.getElementType()) == derefToken(n2.getTokenType());
    }

    public IElementType derefToken(IElementType probablyWrapper) {
      if (probablyWrapper instanceof TokenWrapper) {
        return derefToken(((TokenWrapper)probablyWrapper).getDelegate());
      }
      return probablyWrapper;
    }

    public boolean hashcodesEqual(final ASTNode n1, final LighterASTNode n2) {
      if (n1 instanceof LeafElement && n2 instanceof Token) {
        if (n1 instanceof ForeignLeafPsiElement && n2.getTokenType() instanceof ForeignLeafType) {
          return n1.getText().equals(((ForeignLeafType)n2.getTokenType()).getValue());
        }

        return ((LeafElement)n1).textMatches(myText, ((Token)n2).myTokenStart, ((Token)n2).myTokenEnd);
      }

      if (n1 instanceof PsiErrorElement && n2.getTokenType() == TokenType.ERROR_ELEMENT) {
        final PsiErrorElement e1 = ((PsiErrorElement)n1);
        if (!Comparing.equal(e1.getErrorDescription(), getErrorMessage(n2))) return false;
      }

      return ((TreeElement)n1).hc() == ((Node)n2).hc();
    }
  }

  private class MyTreeStructure implements FlyweightCapableTreeStructure<LighterASTNode> {
    private final LimitedPool<Token> myPool = new LimitedPool<Token>(1000, new LimitedPool.ObjectFactory<Token>() {
      public void cleanup(final Token token) {
        token.myHC = -1;
      }

      public Token create() {
        return new Token();
      }
    });

    private final StartMarker myRoot;

    public MyTreeStructure(final StartMarker root) {
      myRoot = root;
    }

    public LighterASTNode prepareForGetChildren(final LighterASTNode o) {
      return o;
    }

    public LighterASTNode getRoot() {
      return myRoot;
    }

    public void disposeChildren(final LighterASTNode[] nodes, final int count) {
      for (int i = 0; i < count; i++) {
        LighterASTNode node = nodes[i];
        if (node instanceof Token) {
          myPool.recycle((Token)node);
        }
      }
    }

    private int count;
    public int getChildren(final LighterASTNode item, final Ref<LighterASTNode[]> into) {
      if (item instanceof Token || item instanceof ErrorItem) return 0;
      StartMarker marker = (StartMarker)item;

      count = 0;

      ProductionMarker child = marker.firstChild;
      int lexIndex = marker.myLexemIndex;
      while (child != null) {
        lexIndex = insertLeafs(lexIndex, child.myLexemIndex, into);
        ensureCapacity(into);
        into.get()[count++] = child;
        if (child instanceof StartMarker) {
          lexIndex = ((StartMarker)child).myDoneMarker.myLexemIndex;
        }
        child = child.next;
      }
      insertLeafs(lexIndex, marker.myDoneMarker.myLexemIndex, into);

      return count;
    }

    private void ensureCapacity(final Ref<LighterASTNode[]> into) {
      LighterASTNode[] old = into.get();
      if (old == null) {
        old = new LighterASTNode[10];
        into.set(old);
      }
      else if (count >= old.length) {
        LighterASTNode[] newStore = new LighterASTNode[(count * 3) / 2];
        System.arraycopy(old, 0, newStore, 0, count);
        into.set(newStore);
      }
    }


    private int insertLeafs(int curToken, int lastIdx, Ref<LighterASTNode[]> into) {
      lastIdx = Math.min(lastIdx, myLexemCount);
      while (curToken < lastIdx) {
        final int start = myLexStarts[curToken];
        final int end = myLexStarts[curToken + 1];
        final IElementType type = myLexTypes[curToken];
        if (start < end || type instanceof ILeafElementType) { // Empty token. Most probably a parser directive like indent/dedent in phyton
          Token lexem = myPool.alloc();

          lexem.myTokenType = type;
          lexem.myTokenStart = start;
          lexem.myTokenEnd = end;
          ensureCapacity(into);
          into.get()[count++] = lexem;
        }
        curToken++;
      }

      return curToken;
    }
  }

  private class ASTConvertor implements Convertor<Node, ASTNode> {
    private final Node myRoot;

    public ASTConvertor(final Node root) {
      myRoot = root;
    }

    public ASTNode convert(final Node n) {
      if (n instanceof Token) {
        return createLeaf(n.getTokenType(), ((Token)n).myTokenStart, ((Token)n).myTokenEnd);
      }
      else if (n instanceof ErrorItem) {
        final PsiErrorElementImpl errorElement = new PsiErrorElementImpl();
        errorElement.setErrorDescription(((ErrorItem)n).myMessage);
        return errorElement;
      }
      else {
        final CompositeElement composite = n == myRoot ? (CompositeElement)createRootAST((StartMarker)myRoot) : createComposite((StartMarker)n);
        bind(composite, (StartMarker)n);
        return composite;
      }
    }
  }

  public void setDebugMode(boolean dbgMode) {
    myDebugMode = dbgMode;
  }

  public Lexer getLexer() {
    return myLexer;
  }

  @NotNull
  private TreeElement createLeaf(final IElementType type, final int start, final int end) {
    CharSequence text = myCharTable.intern(myText, start, end);
    if (myWhitespaces.contains(type)) {
      return new PsiWhiteSpaceImpl(text);
    }

    if (type instanceof CustomParsingType) {
      return (TreeElement)((CustomParsingType)type).parse(text, myCharTable);
    }

    if (type instanceof ILazyParseableElementType) {
      return ASTFactory.lazy((ILazyParseableElementType)type, text);
    }

    return ASTFactory.leaf(type, text);
  }

  /**
   * just to make removeRange method available.
   */
  private static class MyList extends ArrayList<ProductionMarker> {
    private static final Field ourElementDataField;
    static {
      Field f;
      try {
        f = ArrayList.class.getDeclaredField("elementData");
        f.setAccessible(true);
      } catch(NoSuchFieldException e) {
        LOG.error(e);
        f = null;
      }
      ourElementDataField = f;
    }

    private Object[] cachedElementData;

    public void removeRange(final int fromIndex, final int toIndex) {
      super.removeRange(fromIndex, toIndex);
    }

    MyList() {
      super(256);
    }
    
    public int lastIndexOf(final Object o) {
      if (cachedElementData == null) return super.lastIndexOf(o);
      for (int i = size()-1; i >= 0; i--)
        if (cachedElementData[i]==o) return i;
      return -1;
    }

    public void ensureCapacity(final int minCapacity) {
      if (cachedElementData == null || minCapacity >= cachedElementData.length) {
        super.ensureCapacity(minCapacity);
        initCachedField();
      }
    }

    private void initCachedField() {
      try {
        cachedElementData = (Object[])ourElementDataField.get(this);
      } catch(Exception e) {
        LOG.error(e);
      }
    }
  }
}
