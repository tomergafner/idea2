package com.intellij.refactoring.inline;

import com.intellij.JavaTestUtil;
import com.intellij.codeInsight.TargetElementUtilBase;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.LightCodeInsightTestCase;
import org.jetbrains.annotations.NonNls;

/**
 * @author yole
 */
public class InlineParameterTest extends LightCodeInsightTestCase {
  @Override
  protected String getTestDataPath() {
    return JavaTestUtil.getJavaTestDataPath();
  }

  protected Sdk getProjectJDK() {
    return JavaSdkImpl.getMockJdk15("java 1.5");
  }

  public void testSameValue() throws Exception {
    doTest(true);
  }

  public void testNullValue() throws Exception {
    doTest(true);
  }

  public void testConstructorCall() throws Exception {
    doTest(true);
  }

  public void testStaticFinalField() throws Exception {
    doTest(true);
  }

  public void testRefIdentical() throws Exception {
     doTest(true);
   }

  public void testRefIdenticalNoLocal() throws Exception {
     doTest(false);
   }

  public void testRefLocalConstantInitializer() throws Exception {
     doTest(false);
  }

  public void testRefLocalWithLocal() throws Exception {
     doTest(false);
  }

  public void testRefMethod() throws Exception {
     doTest(true);
  }

  public void testRefMethodOnLocal() throws Exception {
     doTest(false);
  }

  public void testRefFinalLocal() throws Exception {
     doTest(true);
  }

  public void testRefStaticField() throws Exception {
     doTest(true);
  }

  public void testRefFinalLocalInitializedWithMethod() throws Exception {
    doTest(false);
  }

  public void testRefSelfField() throws Exception {
    doTest(false);
  }


  private void doTest(final boolean createLocal) throws Exception {
    getProject().putUserData(InlineParameterExpressionProcessor.CREATE_LOCAL_FOR_TESTS,createLocal);

    String name = getTestName(false);
    @NonNls String fileName = "/refactoring/inlineParameter/" + name + ".java";
    configureByFile(fileName);
    performAction();
    checkResultByFile(null, fileName + ".after", true);
  }

  private static void performAction() {
    final PsiElement element = TargetElementUtilBase.findTargetElement(myEditor, TargetElementUtilBase
      .REFERENCED_ELEMENT_ACCEPTED | TargetElementUtilBase.ELEMENT_NAME_ACCEPTED);
    new InlineParameterHandler().inlineElement(getProject(), myEditor, element);
  }
}
