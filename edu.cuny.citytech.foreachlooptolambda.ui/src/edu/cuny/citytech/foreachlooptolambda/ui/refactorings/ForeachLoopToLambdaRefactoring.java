package edu.cuny.citytech.foreachlooptolambda.ui.refactorings;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.internal.codeassist.ThrownExceptionFinder;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.cuny.citytech.foreachlooptolambda.ui.messages.Messages;
import edu.cuny.citytech.foreachlooptolambda.ui.visitors.EnhancedForStatementVisitor;
import edu.cuny.citytech.refactoring.common.core.Refactoring;

/**
 * The activator class controls the plug-in life cycle
 * 
 * @author <a href="mailto:rkhatchadourian@citytech.cuny.edu">Raffi
 *         Khatchadourian</a>
 */
public class ForeachLoopToLambdaRefactoring extends Refactoring {

	/**
	 * The methods to refactor.
	 */
	private Set<IMethod> methods;

	/**
	 * Creates a new refactoring with the given methods to refactor.
	 * 
	 * @param methods
	 *            The methods to refactor.
	 */
	public ForeachLoopToLambdaRefactoring(IMethod... methods) {
		this.methods = new HashSet<IMethod>(Arrays.asList(methods));
	}

	/**
	 * Default constructor
	 */
	public ForeachLoopToLambdaRefactoring() {
	}

	@Override
	public String getName() {
		// TODO: Please rename.
		return Messages.ForEachLoopToLambdaRefactoring_Name;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		// TODO Empty for now.
		final RefactoringStatus status = new RefactoringStatus();
		return status;
	}

	// this method get the EnhancedForSrarement to check the precondition
	private static Set<EnhancedForStatement> getEnhancedForStatements(IMethod method, IProgressMonitor pm)
			throws JavaModelException {
		ICompilationUnit iCompilationUnit = method.getCompilationUnit();

		// there may be a shared AST already parsed. Let's try to get that
		// one.
		CompilationUnit compilationUnit = RefactoringASTParser.parseWithASTProvider(iCompilationUnit, true,
				new SubProgressMonitor(pm, 1));

		// get the method declaration ASTNode.
		MethodDeclaration methodDeclarationNode = ASTNodeSearchUtil.getMethodDeclarationNode(method, compilationUnit);

		final Set<EnhancedForStatement> statements = new LinkedHashSet<EnhancedForStatement>();
		// extract all enhanced for loop statements in the method.
		methodDeclarationNode.accept(new ASTVisitor() {

			@Override
			public boolean visit(EnhancedForStatement node) {
				statements.add(node);
				return super.visit(node);
			}
		});

		return statements;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		try {
			final RefactoringStatus status = new RefactoringStatus();
			for (IMethod method : methods) {
				Set<EnhancedForStatement> statements = getEnhancedForStatements(method, new SubProgressMonitor(pm, 1));

				IProgressMonitor subMonitor = new SubProgressMonitor(pm, statements.size());

				// check preconditions on each.
				statements.stream().forEach(s -> status.merge(checkEnhancedForStatement(s, subMonitor)));
				pm.worked(1);
			}
			return status;
		} finally {
			pm.done();
		}
	}

	// Checking if the EnhancedForLoop iterate over collection
	private static boolean checkEnhancedForStatementIteratesOverCollection(EnhancedForStatement enhancedForStatement,
			IProgressMonitor pm) {
		boolean isNotInstanceOfCollection = true;

		Expression expression = enhancedForStatement.getExpression();
		ITypeBinding nodeBindingType = expression.resolveTypeBinding();

		if (nodeBindingType.isArray()) {
			isNotInstanceOfCollection = true;
		} else {
			// STEP 1: getting java the element of the type,
			IType iTypeElement = (IType) nodeBindingType.getJavaElement();
			// Debug Purpose: will be remove once code is done
			System.out.println("This is ITypeElement " + iTypeElement);

			try {
				// STEP 2: getting java iTypeHeirchay,
				ITypeHierarchy iTypeHeirchay = iTypeElement.newSupertypeHierarchy(pm);
				// Debug Purpose: will be remove once code is done
				System.out.println("this is ITypeHeirchay " + iTypeHeirchay);
				// STEP 3:
				IType[] iType = iTypeHeirchay.getAllInterfaces();
				// Debug Purpose: will be remove once code is done
				for (IType iType2 : iType) {
					System.out.println(iType2);
				}
				// ---------Debug---------------//
			} catch (JavaModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		String typeName = nodeBindingType.getQualifiedName();
		//passing the default method by comparing List
		if ((typeName.startsWith("java.util.Collection"))||(typeName.startsWith("java.util.List"))) {
			isNotInstanceOfCollection = false;
		}

		return isNotInstanceOfCollection;
	}

	// getting any uncaught exception
	public void checkException() {
		ThrownExceptionFinder thrownUncaughtExceptions = new ThrownExceptionFinder();
		ReferenceBinding[] thrownUncaughtException = thrownUncaughtExceptions.getThrownUncaughtExceptions();
		if (thrownUncaughtException.length > 0) {

		}
	}

	// Checking with the precondiiton,
	private static RefactoringStatus checkEnhancedForStatement(EnhancedForStatement enhancedForStatement,
			IMethod method, IProgressMonitor pm) {
		try {
			RefactoringStatus status = new RefactoringStatus();
			// create the visitor.
			EnhancedForStatementVisitor visitor = new EnhancedForStatementVisitor();
			// have the AST node "accept" the visitor.
			enhancedForStatement.accept(visitor);

			final Set<String> warningStatement = new HashSet<String>();
			if (visitor.containsBreak()) {
				addWarning(status, Messages.ForEachLoopToLambdaRefactoring_ContainBreak, method);
			}

			if (visitor.containsContinue()) {
				addWarning(status, Messages.ForEachLoopToLambdaRefactoring_ContainContinue, method);			
			}

			if (visitor.containsInvalidReturn()) {
				addWarning(status, Messages.ForEachLoopToLambdaRefactoring_ContainInvalidReturn, method);
			}
			
			if (visitor.containsMultipleReturn()) {
				addWarning(status, Messages.ForEachLoopToLambdaRefactoring_ContainMultipleReturn, method);			
			}

			if (visitor.containsException()) {
				addWarning(status, Messages.ForEachLoopToLambdaRefactoring_ContainException, method);
			}

			if (checkEnhancedForStatementIteratesOverCollection(enhancedForStatement, pm)) {
				addWarning(status, Messages.ForEachLoopToLambdaRefactoring_IteratesOverCollection, method);
			}
			
			
			//status.merge(checkMethodBody(method, new SubProgressMonitor(pm, 1)));
			pm.worked(1);
			return status; // passed.
		} finally {
			pm.done();
		}
	}

	protected static RefactoringStatus checkMethodBody(IMethod method, IProgressMonitor pm) {
		RefactoringStatus status = new RefactoringStatus();

		ITypeRoot root = method.getCompilationUnit();
		CompilationUnit unit = RefactoringASTParser.parseWithASTProvider(root, false, new SubProgressMonitor(pm, 1));

		MethodDeclaration declaration;
		try {
			declaration = ASTNodeSearchUtil.getMethodDeclarationNode(method, unit);
			if (declaration != null) {
				Block body = declaration.getBody();

				if (body != null) {
					@SuppressWarnings("rawtypes")
					List statements = body.statements();

					if (!statements.isEmpty()) {
						// TODO for now.
						addWarning(status, Messages.ForEachLoopToLambdaRefactoring_NoMethodsWithStatements, method);
					}
				}
			}
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return status;
	}

	protected static void addWarning(RefactoringStatus status, String message, IMethod method) {
		status.addWarning(MessageFormat.format(message, method.getElementName()), JavaStatusContext.create(method));
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {

		try {
			pm.beginTask(Messages.ForEachLoopToLambdaRefactoring_CreatingChange, 1);

			return new NullChange(getName());
		} finally {
			pm.done();
		}
	}
}
