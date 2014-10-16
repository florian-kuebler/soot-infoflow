package soot.jimple.infoflow.data.pathBuilders;

import heros.solver.CountingThreadPoolExecutor;

import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.InfoflowResults;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.data.ILinkedAbstraction;
import soot.jimple.infoflow.data.SourceContextAndPath;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

/**
 * Class for reconstructing abstraction paths from sinks to source. This builder
 * is context-insensitive which makes it faster, but also less precise than
 * {@link ContextSensitivePathBuilder}.
 * 
 * @author Steven Arzt
 */
public class ContextInsensitivePathBuilder<D extends ILinkedAbstraction<D>> extends AbstractAbstractionPathBuilder<D> {
	
	private AtomicInteger propagationCount = null;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final InfoflowResults results = new InfoflowResults();
	private final CountingThreadPoolExecutor executor;
	
	private boolean reconstructPaths = false;
		
	/**
	 * Creates a new instance of the {@link ContextSensitivePathBuilder} class
	 * @param maxThreadNum The maximum number of threads to use
	 */
	public ContextInsensitivePathBuilder(BiDiInterproceduralCFG<Unit,SootMethod> icfg, int maxThreadNum) {
		super(icfg);
        int numThreads = Runtime.getRuntime().availableProcessors();
		this.executor = createExecutor(maxThreadNum == -1 ? numThreads
				: Math.min(maxThreadNum, numThreads));
	}
	
	/**
	 * Creates a new executor object for spawning worker threads
	 * @param numThreads The number of threads to use
	 * @return The generated executor
	 */
	private CountingThreadPoolExecutor createExecutor(int numThreads) {
		return new CountingThreadPoolExecutor
				(numThreads, Integer.MAX_VALUE, 30, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());
	}
	
	/**
	 * Task for tracking back the path from sink to source.
	 * 
	 * @author Steven Arzt
	 */
	private class SourceFindingTask implements Runnable {
		private final ILinkedAbstraction<D> abstraction;
		
		public SourceFindingTask(ILinkedAbstraction<D> abstraction) {
			this.abstraction = abstraction;
		}
		
		@Override
		public void run() {
			propagationCount.incrementAndGet();
			
			final Set<SourceContextAndPath> paths = abstraction.getPaths();
			final D pred = abstraction.getPredecessor();
			
			if (pred == null) {
				// If we have no predecessors, this must be a source
				assert abstraction.getSourceContext() != null;
				assert abstraction.getNeighbors() == null;
				
				// Register the result
				for (SourceContextAndPath scap : paths) {
					SourceContextAndPath extendedScap =
							scap.extendPath(abstraction.getSourceContext().getStmt());
					results.addResult(extendedScap.getValue(),
							extendedScap.getStmt(),
							abstraction.getSourceContext().getValue(),
							abstraction.getSourceContext().getStmt(),
							abstraction.getSourceContext().getUserData(),
							extendedScap.getPath());
				}
			}
			else {
				for (SourceContextAndPath scap : paths) {						
					// Process the predecessor
					if (processPredecessor(scap, pred))
						// Schedule the predecessor
						executor.execute(new SourceFindingTask(pred));
					
					// Process the predecessor's neighbors
					if (pred.getNeighbors() != null)
						for (D neighbor : pred.getNeighbors())
							if (processPredecessor(scap, neighbor))
								// Schedule the predecessor
								executor.execute(new SourceFindingTask(neighbor));
				}
			}
		}

		private boolean processPredecessor(SourceContextAndPath scap, D pred) {
			// Put the current statement on the list
			SourceContextAndPath extendedScap = scap.extendPath(reconstructPaths
					? pred.getCurrentStmt() : null);
			
			// Add the new path
			return pred.addPathElement(extendedScap);
		}
	}
	
	@Override
	public void computeTaintSources(final Set<AbstractionAtSink<D>> res) {
		this.reconstructPaths = false;
		runSourceFindingTasks(res);
	}
	
	@Override
	public void computeTaintPaths(final Set<AbstractionAtSink<D>> res) {
		this.reconstructPaths = true;
		runSourceFindingTasks(res);
	}
	
	private void runSourceFindingTasks(final Set<AbstractionAtSink<D>> res) {
		if (res.isEmpty())
			return;
		
		long beforePathTracking = System.nanoTime();
		propagationCount = new AtomicInteger();
    	logger.info("Obtainted {} connections between sources and sinks", res.size());
    	
    	// Start the propagation tasks
    	int curResIdx = 0;
    	for (final AbstractionAtSink<D> abs : res) {
    		logger.info("Building path " + ++curResIdx);
   			buildPathForAbstraction(abs);
   			
   			// Also build paths for the neighbors of our result abstraction
   			if (abs.getAbstraction().getNeighbors() != null)
   				for (D neighbor : abs.getAbstraction().getNeighbors()) {
   					AbstractionAtSink<D> neighborAtSink = new AbstractionAtSink<D>(neighbor,
   							abs.getSinkValue(), abs.getSinkStmt());
   		   			buildPathForAbstraction(neighborAtSink);
   				}
    	}

    	try {
			executor.awaitCompletion();
		} catch (InterruptedException ex) {
			logger.error("Could not wait for path executor completion: {0}", ex.getMessage());
			ex.printStackTrace();
		}
    	
    	logger.info("Path processing took {} seconds in total for {} edges",
    			(System.nanoTime() - beforePathTracking) / 1E9, propagationCount.get());
	}
	
	/**
	 * Builds the path for the given abstraction that reached a sink
	 * @param abs The abstraction that reached a sink
	 */
	private void buildPathForAbstraction(final AbstractionAtSink<D> abs) {
		SourceContextAndPath scap = new SourceContextAndPath(
				abs.getSinkValue(), abs.getSinkStmt());
		scap = scap.extendPath(abs.getSinkStmt());
		abs.getAbstraction().addPathElement(scap);
		
		executor.execute(new SourceFindingTask(abs.getAbstraction()));
	}
	
	@Override
	public void shutdown() {
    	executor.shutdown();		
	}

	@Override
	public InfoflowResults getResults() {
		return this.results;
	}

}
