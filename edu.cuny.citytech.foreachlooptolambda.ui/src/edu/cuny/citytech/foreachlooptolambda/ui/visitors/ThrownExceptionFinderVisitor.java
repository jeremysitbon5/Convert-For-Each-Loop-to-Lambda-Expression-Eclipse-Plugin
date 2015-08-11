/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package edu.cuny.citytech.foreachlooptolambda.ui.visitors;

import java.util.HashSet;
import java.util.Stack;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThrowStatement;

public class ThrownExceptionFinderVisitor extends ASTVisitor {

	private HashSet thrownExceptions;
	private Stack exceptionsStack;
	private HashSet caughtExceptions;
	private HashSet discouragedExceptions;

	/**
	 * Finds the thrown exceptions minus the ones that are already caught in
	 * previous statement. Exception is already caught even if its super type is
	 * being caught. Also computes, separately, a list comprising of (a)those
	 * exceptions that have been caught already and (b)those exceptions that are
	 * thrown by the method and whose super type has been caught already.
	 * 
	 * @param satement
	 * @param scope
	 */
	public void processThrownExceptions(Statement statement) {
		this.thrownExceptions = new HashSet<Statement>();
		this.exceptionsStack = new Stack<HashSet<?>>();
		this.caughtExceptions = new HashSet<Statement>();
		this.discouragedExceptions = new HashSet<Statement>();
		//statement.traverse(this, scope);
		//removeCaughtExceptions(tryStatement,true /* remove unchecked exceptions this time */);
	}
	
	private void acceptException(IMethodBinding binding) {
		if (binding != null) {
			this.thrownExceptions.add(binding);
		}
	}
	
	public void endVisit(ThrowStatement throwStatement) {
		acceptException((IMethodBinding) (throwStatement));
		super.endVisit(throwStatement);
	}
	
	public IMethodBinding[] getAlreadyCaughtExceptions() {
		IMethodBinding[] allCaughtExceptions = new IMethodBinding[this.caughtExceptions.size()];
		this.caughtExceptions.toArray(allCaughtExceptions);
		return allCaughtExceptions;
	}
	
	public IMethodBinding[] getThrownUncaughtExceptions() {
		IMethodBinding[] result = new IMethodBinding[this.thrownExceptions.size()];
		this.thrownExceptions.toArray(result);
		return result;
	}
	
	
	
	public boolean visit(Statement statement) {
		this.exceptionsStack.push(this.thrownExceptions);
		HashSet<Statement> exceptionSet = new HashSet<Statement>();
		this.thrownExceptions = exceptionSet;
		//statement.BLOCK.traverse(this, scope);

		this.thrownExceptions = (HashSet)this.exceptionsStack.pop();

		Object[] values = exceptionSet.toArray();
		for (int i = 0; i < values.length; i++) {
			if (values[i] != null) {
				this.thrownExceptions.add(values[i]);
			}
		}
		
		return false;
	}
	
}