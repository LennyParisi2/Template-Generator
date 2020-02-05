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
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
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
		
		
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();
        // Get all projects in the workspace
        IProject[] projects = root.getProjects();
        // Loop over all projects
        for (IProject project : projects) {
            try {
                printProjectInfo(project);
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
		
		
		
		
		// ============================================================================================================
        // The execute method of the handler is invoked to handle the event. As we only contribute to Explorer
        // Navigator views we expect to get a selection tree event
        // ============================================================================================================
//        this.window = HandlerUtil.getActiveWorkbenchWindow(event);
//        // Get the active WorkbenchPage
//        this.activePage = this.window.getActivePage();
//
//        // Get the Selection from the active WorkbenchPage page
//        ISelection selection = this.activePage.getSelection();
//        if(selection instanceof ITreeSelection) {
//            TreeSelection treeSelection = (TreeSelection) selection;
//            TreePath[] treePaths = treeSelection.getPaths();
//            TreePath treePath = treePaths[0];
//		
//            Object file = treePath.getLastSegment();
//            
//            if(file instanceof ICompilationUnit) {
//            	ICompilationUnit unit = (ICompilationUnit)file;
//            	try {
//					printFile(unit);
//				} catch (JavaModelException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//            }
//            
//        }else {
//        	MessageDialog.openError(this.window.getShell(), "Error", "Please select a file from the project explorer");
//        }
		
		return null;
	}


    private void printProjectInfo(IProject project) throws CoreException,
            JavaModelException {
        System.out.println("Working in project " + project.getName());
        // check if we have a Java project
        if (project.isNatureEnabled("org.eclipse.jdt.core.javanature")) {
            IJavaProject javaProject = JavaCore.create(project);
            printPackageInfos(javaProject);
        }
    }

    private void printPackageInfos(IJavaProject javaProject)
            throws JavaModelException {
        IPackageFragment[] packages = javaProject.getPackageFragments();
        for (IPackageFragment mypackage : packages) {
            // Package fragments include all packages in the
            // classpath
            // We will only look at the package from the source
            // folder
            // K_BINARY would include also included JARS, e.g.
            // rt.jar
            if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
                System.out.println("Package " + mypackage.getElementName());
                printICompilationUnitInfo(mypackage);

            }

        }
    }
	private void printICompilationUnitInfo(IPackageFragment mypackage)
            throws JavaModelException {
        for (ICompilationUnit unit : mypackage.getCompilationUnits()) {
            printCompilationUnitDetails(unit);

        }
    }

    private void printIMethods(ICompilationUnit unit) throws JavaModelException {
        IType[] allTypes = unit.getAllTypes();
        for (IType type : allTypes) {
        	if(type.isClass()) {
            	System.out.println("\nEvaluating type: " + type.getElementName());
                System.out.println("Fields:");
                printIClassDetails(type);
                System.out.println("Methods:");
                printIMethodDetails(type);
        	}
        }
    }

    private void printCompilationUnitDetails(ICompilationUnit unit)
            throws JavaModelException {
        System.out.println("Source file " + unit.getElementName());
        Document doc = new Document(unit.getSource());
        System.out.println("Has number of lines: " + doc.getNumberOfLines());
        printIMethods(unit);
    }

    private void printIMethodDetails(IType type) throws JavaModelException {
        IMethod[] methods = type.getMethods();
        for (IMethod method : methods) {
            System.out.println("Method name " + method.getElementName());
            System.out.println("Signature :" + method.getSignature().toString());
            System.out.println("Return Type " + Signature.getSignatureSimpleName(method.getReturnType()));

        }
    }
    
    private void printIClassDetails(IType type) throws JavaModelException {
        IField[] methods = type.getFields();
        for (IField field : methods) {
            System.out.println("Field name: " + field.getElementName());
            System.out.println("Type: " + Signature.getSignatureSimpleName(field.getTypeSignature()));
        }
    }
    
}
