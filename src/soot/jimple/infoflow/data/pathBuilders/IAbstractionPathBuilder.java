package soot.jimple.infoflow.data.pathBuilders;

import java.util.Set;

import soot.jimple.infoflow.InfoflowResults;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.data.ILinkedAbstraction;

/**
 * Common interface for all path construction algorithms. These algorithms
 * reconstruct the path from the sink to the source.
 *  
 * @author Steven Arzt
 */
public interface IAbstractionPathBuilder<D extends ILinkedAbstraction<D>> {

	/**
	 * Finds the sources from which data flows to the sinks
	 * @param res The data flow tracker results
	 */
	public void computeTaintSources(final Set<AbstractionAtSink<D>> res);

	/**
	 * Computes the path of tainted data between the source and the sink
	 * @param res The data flow tracker results
	 */
	public void computeTaintPaths(final Set<AbstractionAtSink<D>> res);
	
	/**
	 * Gets the constructed result paths
	 * @return The constructed result paths
	 */
	public InfoflowResults getResults();

	/**
	 * Shuts down the path processing
	 */
	public void shutdown();
	
}
