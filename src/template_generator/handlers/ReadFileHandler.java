package template_generator.handlers;


import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.Document;


public class ReadFileHandler extends AbstractHandler {
	private IWorkbenchWindow window;
    private IWorkbenchPage activePage;

    private IProject theProject;
    private IResource theResource;
    private IFile theFile;

    private String workspaceName;
    private String projectName;
    private String fileName;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		// ============================================================================================================
        // The execute method of the handler is invoked to handle the event. As we only contribute to Explorer
        // Navigator views we expect to get a selection tree event
        // ============================================================================================================
        this.window = HandlerUtil.getActiveWorkbenchWindow(event);
        // Get the active WorkbenchPage
        this.activePage = this.window.getActivePage();

        // Get the Selection from the active WorkbenchPage page
        ISelection selection = this.activePage.getSelection();
        if(selection instanceof ITreeSelection) {
            TreeSelection treeSelection = (TreeSelection) selection;
            TreePath[] treePaths = treeSelection.getPaths();
            TreePath treePath = treePaths[0];
		
            Object file = treePath.getLastSegment();
            
            if(file instanceof ICompilationUnit) {
            	ICompilationUnit unit = (ICompilationUnit)file;
            	try {
					printFile(unit);
				} catch (JavaModelException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
            
        }else {
        	MessageDialog.openError(this.window.getShell(), "Error", "Please select a file from the project explorer");
        }
		
		MessageDialog.openInformation(
				this.window.getShell(),
				"Template_Generator",
				"Hello, Eclipse world");
		
		return null;
	}
	
	private void printFile(ICompilationUnit unit) throws JavaModelException {
		Document doc = new Document(unit.getSource());
        System.out.println("Has number of lines: " + doc.getNumberOfLines());
        printIMethods(unit);
	}
	

	private void printIMethods(ICompilationUnit unit) throws JavaModelException {
        IType[] allTypes = unit.getAllTypes();
        for (IType type : allTypes) {
            printIMethodDetails(type);
        }
    }
	
    private void printIMethodDetails(IType type) throws JavaModelException {
        IMethod[] methods = type.getMethods();
        for (IMethod method : methods) {

            System.out.println("Method name " + method.getElementName());
            System.out.println("Signature " + method.getSignature());
            System.out.println("Return Type " + method.getReturnType());

        }
    }
}
