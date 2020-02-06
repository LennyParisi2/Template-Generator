package template_generator.handlers;

import static org.eclipse.jdt.core.dom.AST.JLS8;
import static org.eclipse.jdt.core.dom.ASTParser.newParser;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

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
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;


public class ReadFileHandler extends AbstractHandler {

    private IWorkbenchWindow window;
    private IWorkbenchPage activePage;
	
    // gets called when command is executed (CNTRL + 6)
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String selection = getSelectedFile(event);
		if(selection!=null) {
			JavaProject selectedProject = getSelectedProject(event);
			if(selectedProject!=null) {
				try {
					ICompilationUnit selectedIComp = getICompFromProject(selectedProject.getPackageFragments(),selection);
					if(selectedIComp!=null) {
						addCommentTo(selectedIComp,"//HELLO WORLD!!");
					}
				} catch (JavaModelException | IllegalArgumentException | MalformedTreeException | BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}	
		}else {
			MessageDialog.openError(this.window.getShell(), "Error", "Please select a file from the project explorer.");
		}
		
        return null;
	}
	
	// gets the name of the selected file, and returns null if a file was not selected
	private String getSelectedFile(ExecutionEvent event) {
		this.window = HandlerUtil.getActiveWorkbenchWindow(event); 
		// Get the active WorkbenchPage
        this.activePage = this.window.getActivePage();
        
     // Get the Selection from the active WorkbenchPage page
        ISelection selection = this.activePage.getSelection();
        if(selection instanceof ITreeSelection) {
            TreeSelection treeSelection = (TreeSelection) selection;
            TreePath[] treePaths = treeSelection.getPaths();
            TreePath treePath = treePaths[0];
            
            Object fileSelection = treePath.getLastSegment();
            if(fileSelection instanceof org.eclipse.jdt.internal.core.CompilationUnit) {
            	org.eclipse.jdt.internal.core.CompilationUnit unit = (org.eclipse.jdt.internal.core.CompilationUnit)fileSelection;
            	return unit.getElementName();
            }
        }
        return null;
	}

	// gets the selected project, and returns null if file was not selected
	private JavaProject getSelectedProject(ExecutionEvent event) {
		this.window = HandlerUtil.getActiveWorkbenchWindow(event); 
		// Get the active WorkbenchPage
        this.activePage = this.window.getActivePage();
        // Get the Selection from the active WorkbenchPage page
        ISelection selection = this.activePage.getSelection();
        TreeSelection treeSelection = (TreeSelection) selection;
        TreePath[] treePaths = treeSelection.getPaths();
        TreePath treePath = treePaths[0];
        Object projectSelected = treePath.getFirstSegment();
        if(projectSelected instanceof JavaProject) {
        	JavaProject unit = (JavaProject)projectSelected;
        	return unit;
        }
        return null;
	}
	
	// get ICOMP selected
	private ICompilationUnit getICompFromProject(IPackageFragment[] packages,String selectedFileName) throws JavaModelException {
		for (IPackageFragment mypackage : packages) {
            if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
            	for (ICompilationUnit unit : mypackage.getCompilationUnits()) {
                    if(unit.getElementName().equals(selectedFileName)) {
                    	return unit;
                    }
                }
            }

        }
		return null;
	}

	// write a comment to a given ICOMP file
	private void addCommentTo(ICompilationUnit unit,String comment) throws JavaModelException, IllegalArgumentException, MalformedTreeException, BadLocationException {

		// read the whole file
		CompilationUnit astRoot = parse(unit);
		
		//create a ASTRewrite
		AST ast = astRoot.getAST();
		ASTRewrite rewriter = ASTRewrite.create(ast);
	 
		// get all type declarations on the file
		List<TypeDeclaration> types = astRoot.types();
		
		// apply the text edits to the compilation unit
		Document document = new Document(unit.getSource());
		
		
		// go through each type declaration
		for(TypeDeclaration type : types) {
			// ensure its not an interface (we want to ignore those)
			if(type.isInterface()) continue;
			
			// all the fields in this class
			String fieldsTemplateSection = generateFieldsTemplateSection(type.getFields());
			// all the methods in this class
			MethodDeclaration[] methods = type.getMethods();
			String methodsTemplateSection = generateMethodTemplateSection(methods);
			String template = "\n/*TEMPLATE \n"+fieldsTemplateSection + "\n * "+methodsTemplateSection+"*/";
			
			// add the comment to an object that can then be written into the AST
			System.out.println("Writing to: " + type.getName());
			ListRewrite listRewrite = rewriter.getListRewrite(methods[methods.length-1].getBody(), (ChildListPropertyDescriptor) Block.propertyDescriptors(Block.BLOCK_COMMENT).get(0));				
			Statement placeHolder = (Statement) rewriter.createStringPlaceholder(template, ASTNode.EMPTY_STATEMENT);
			listRewrite.insertFirst(placeHolder, null);
			
			
		}

		// error is thrown here D:
		rewriter.rewriteAST().apply(document);
		
		//save changes
		unit.getBuffer().setContents(document.get());
	}
	
	// generates the fields section of a template 
	private String generateFieldsTemplateSection(FieldDeclaration[] fields) {
		String result = fields.length>0? " * FIELDS: \n":"";
		for(FieldDeclaration field : fields) {
			String fullField = field.toString();
			fullField = fullField.replaceAll(";", "");
			fullField = fullField.replaceAll("\n", "");
			if(fullField.toString().contains(" =")) {
				fullField = fullField.substring(0,fullField.indexOf(" ="));
			}else if(fullField.toString().contains("=")) {
				fullField = fullField.substring(0,fullField.indexOf("="));
			}
			
			String[] parts = fullField.toString().split(" ");
			String type = parts[parts.length-2];
			String name = parts[parts.length-1];
			result += " * ... this." + name+ " ...     - " + type + "\n";
		}
		return result;
	}
	
	private String generateMethodTemplateSection(MethodDeclaration[] methods) {
		String result = methods.length>0? " * METHODS: \n":"";
		for(MethodDeclaration method : methods) {
			String paramsString = "(";
			List<Object> params = method.parameters();
			if(params.size()>0) {
				for(Object p : params)
				{
					String[] paramParts = p.toString().split(" ");
					String type = paramParts[paramParts.length-2];
					paramsString += type +", ";
				}
				
				paramsString = paramsString.substring(0,paramsString.length()-1);
			}
			paramsString += ")";
			
			result += " * ... this." + method.getName()+ paramsString + " ...     - " + method.getReturnType2() + "\n";
		}
		return result;
	}

	// parses an ICompilationUnit to a CompilationUnit
	private static CompilationUnit parse(ICompilationUnit unit) {
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(unit);
        parser.setResolveBindings(true);
        return (CompilationUnit) parser.createAST(null); // parse
    }

}
