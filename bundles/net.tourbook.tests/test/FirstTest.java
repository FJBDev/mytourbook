import static org.junit.Assert.assertFalse;

import org.eclipse.reddeer.eclipse.jdt.ui.wizards.JavaProjectWizard;
import org.eclipse.reddeer.eclipse.jdt.ui.wizards.NewClassCreationWizard;
import org.eclipse.reddeer.eclipse.jdt.ui.wizards.NewClassWizardPage;
import org.eclipse.reddeer.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne;
import org.eclipse.reddeer.eclipse.ui.views.markers.ProblemsView;
import org.eclipse.reddeer.eclipse.ui.views.markers.ProblemsView.ProblemType;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.workbench.impl.editor.TextEditor;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(RedDeerSuite.class)
public class FirstTest {

   @Test
   public void test() {
//https://www.vogella.com/tutorials/EclipseRedDeer/article.html
      // Create Java Project
      final JavaProjectWizard projectDlg = new JavaProjectWizard();
      projectDlg.open();
      final NewJavaProjectWizardPageOne projectPage = new NewJavaProjectWizardPageOne(projectDlg);
      projectPage.setProjectName("testProject");
      projectDlg.finish();

      // Create Java class
      final NewClassCreationWizard classDlg = new NewClassCreationWizard();
      classDlg.open();
      final NewClassWizardPage classPage = new NewClassWizardPage(classDlg);
      classPage.setName("RedDeerDemo");
      classPage.setPackage("org.reddeer.demo");
      classDlg.finish();

      // Edit Java class
      final TextEditor textEditor = new TextEditor("RedDeerDemo.java");
      textEditor.setText("Written by RedDeer");
      textEditor.save();

      // Check ProblemsView
      final ProblemsView problemsView = new ProblemsView();
      problemsView.open();
      assertFalse(problemsView.getProblems(ProblemType.ERROR).isEmpty());
   }

}
