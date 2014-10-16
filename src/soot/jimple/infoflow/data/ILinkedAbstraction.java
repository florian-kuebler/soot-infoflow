package soot.jimple.infoflow.data;

import java.util.Set;

import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.solver.fastSolver.FastSolverLinkedNode;

public interface ILinkedAbstraction<D extends ILinkedAbstraction<D>> extends FastSolverLinkedNode<D, Unit> {

	D getPredecessor();

	SourceContext getSourceContext();

	Set<D> getNeighbors();

	Stmt getCurrentStmt();

	Stmt getCorrespondingCallSite();

	boolean addPathElement(SourceContextAndPath scap);

	/**
	 * Registers that a worker thread with the given ID has already processed
	 * this abstraction
	 * @param id The ID of the worker thread
	 * @return True if the worker thread with the given ID has not been
	 * registered before, otherwise false
	 */
	boolean registerPathFlag(int id);
	
	/**
	 * Gets the path of statements from the source to the current statement
	 * with which this abstraction is associated. If this path is ambiguous,
	 * a single path is selected randomly.
	 * @return The path from the source to the current statement
	 */
	Set<SourceContextAndPath> getPaths();

}
