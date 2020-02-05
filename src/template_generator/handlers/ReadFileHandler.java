package template_generator.handlers;

import static org.eclipse.jdt.core.dom.AST.JLS8;
import static org.eclipse.jdt.core.dom.ASTParser.newParser;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;


public class ReadFileHandler extends AbstractHandler {

	private String templateComment;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();
        // Get all projects in the workspace
        IProject[] projects = root.getProjects();
        // Loop over all projects
        for (IProject project : projects) {
            try {
                analyzeFields(project);
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
		return null;
	}
	
	/* TEMP */
	private void analyzeFields(IProject project) throws JavaModelException {
        IPackageFragment[] packages = JavaCore.create(project).getPackageFragments();
        // parse(JavaCore.create(project));
        for (IPackageFragment mypackage : packages) {
            if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
                createAST(mypackage);
            }

        }
    }

    private void createAST(IPackageFragment mypackage) throws JavaModelException {
        for (ICompilationUnit unit : mypackage.getCompilationUnits()) {
            // now create the AST for the ICompilationUnits
            CompilationUnit parse = parse(unit);
            FieldVisitor visitor = new FieldVisitor();
            parse.accept(visitor);

            for (FieldDeclaration fieldDeclaration : visitor.getFields()) {
                System.out.print(fieldDeclaration.toString());
            }

        }
    }

    /**
     * Reads a ICompilationUnit and creates the AST DOM for manipulating the
     * Java source file
     *
     * @param unit
     * @return
     */

    private static CompilationUnit parse(ICompilationUnit unit) {
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(unit);
        parser.setResolveBindings(true);
        return (CompilationUnit) parser.createAST(null); // parse
    }
    
    //============================================================================


    private void printProjectInfo(IProject project) throws CoreException,
            JavaModelException, MalformedTreeException, BadLocationException {
        System.out.println("Working in project " + project.getName());
        // check if we have a Java project
        if (project.isNatureEnabled("org.eclipse.jdt.core.javanature")) {
            IJavaProject javaProject = JavaCore.create(project);
            printPackageInfos(javaProject);
        }
    }

    private void printPackageInfos(IJavaProject javaProject)
            throws JavaModelException, MalformedTreeException, BadLocationException {
        IPackageFragment[] packages = javaProject.getPackageFragments();
        for (IPackageFragment mypackage : packages) {
            if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
                System.out.println("Package " + mypackage.getElementName());
                printICompilationUnitInfo(mypackage);

            }

        }
    }
	private void printICompilationUnitInfo(IPackageFragment mypackage)
            throws JavaModelException, MalformedTreeException, BadLocationException {
        for (ICompilationUnit unit : mypackage.getCompilationUnits()) {
        	printCompilationUnitDetails(unit);
        }
    }

	private void printIMethods(ICompilationUnit unit) throws JavaModelException {
        IType[] allTypes = unit.getAllTypes();
        for (IType type : allTypes) {
        	if(type.isClass()) {
        		this.templateComment = "/*TEMPLATE\n";
            	System.out.println("\n"+type.getElementName());
                this.templateComment += " * FIELDS: \n";
                printIClassDetails(type);
                this.templateComment += " * METHODS: \n";
                printIMethodDetails(type);
            	this.templateComment += "*/";
                System.out.println(this.templateComment);
        	}
        }
    }

    private void printCompilationUnitDetails(ICompilationUnit unit)
            throws JavaModelException {
//        Document doc = new Document(unit.getSource());
        printIMethods(unit);
    }

    private void printIMethodDetails(IType type) throws JavaModelException {	
        IMethod[] methods = type.getMethods();
        for (IMethod method : methods) {
        	this.templateComment += " * ... this."+method.getElementName() +"("+Signature.getSignatureSimpleName(method.getSignature()).split(" \\(")[0]+") ...    - " + Signature.getSignatureSimpleName(method.getReturnType())+"\n";
        }
        

    	String parentClassName = type.getSuperclassName();
        if(parentClassName!=null) {
        	// Call this on the parent class basically
        }
        
    }

    private void getMethodDetailsOf(Class<?> type) {
    	System.out.println("Getting class methods for: " + type.getSimpleName());
    	Method[] methods = type.getMethods();

        for (Method method : methods) {
        	
        	String params = "";
        	for(Parameter param :  method.getParameters()) {
        		params += Signature.getSignatureSimpleName(param.getName())+",";
        	}
        	
        	this.templateComment += " * ... this."+method.getName() +"("+params+") ...    - " + method.getReturnType().getSimpleName()+"\n";
        }
        
        if(type.getSuperclass()!=null) {
        	System.out.println("Getting superclass: " + type.getSuperclass().getSimpleName());
        	getMethodDetailsOf(type.getSuperclass());
        }
    }
    
    
    
    private void printIClassDetails(IType type) throws JavaModelException {
        IField[] fields = type.getFields();
        for (IField field : fields) {
        	this.templateComment += " * ... this."+field.getElementName()+" ...   - " + Signature.getSignatureSimpleName(field.getTypeSignature())+"\n";
        }
    }
    
    private void addComment(String comment,ICompilationUnit element) {
    	ASTParser parser = ASTParser.newParser(AST.JLS8);
        ​parser.setResolveBindings(true);
        ​parser.setKind(ASTParser.K_COMPILATION_UNIT);
        ​parser.setBindingsRecovery(true);
        parser.setSource(element);
        ​CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);
    }
}
