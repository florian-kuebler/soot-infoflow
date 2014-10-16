package soot.jimple.infoflow.data.pathBuilders;

import heros.solver.CountingThreadPoolExecutor;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowResults;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.data.ILinkedAbstraction;

/**
 * Class for reconstructing abstraction paths from sinks to source
 * 
 * @author Steven Arzt
 */
public class ContextInsensitiveSourceFinder<D extends ILinkedAbstraction<D>> implements IAbstractionPathBuilder<D> {
	
	private AtomicInteger propagationCount = null;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final InfoflowResults results = new InfoflowResults();
	private final CountingThreadPoolExecutor executor;
	
	private static int lastTaskId = 0;
	
	/**
	 * Creates a new instance of the {@link ContextInsensitiveSourceFinder} class
	 * @param maxThreadNum The maximum number of threads to use
	 */
	public ContextInsensitiveSourceFinder(int maxThreadNum) {
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
	 * Task for only finding sources, not the paths towards them
	 * 
	 * @author Steven Arzt
	 */
	private class SourceFindingTask implements Runnable {
		private final int taskId;
		private final AbstractionAtSink<D> flagAbs;
		private final List<D> abstractionQueue = new LinkedList<D>();
		
		public SourceFindingTask(int taskId, AbstractionAtSink<D> flagAbs, D abstraction) {
			this.taskId = taskId;
			this.flagAbs = flagAbs;
			this.abstractionQueue.add(abstraction);
		}
		
		@Override
		public void run() {
			while (!abstractionQueue.isEmpty()) {
				D abstraction = abstractionQueue.remove(0);
				propagationCount.incrementAndGet();
								
				if (abstraction.getSourceContext() != null) {
					// Register the result
					results.addResult(flagAbs.getSinkValue(),
							flagAbs.getSinkStmt(),
							abstraction.getSourceContext().getValue(),
							abstraction.getSourceContext().getStmt(),
							abstraction.getSourceContext().getUserData(),
							Collections.<Stmt>emptyList());
					
					// Sources may not have predecessors
					assert abstraction.getPredecessor() == null;
				}
				else
					if (abstraction.getPredecessor().registerPathFlag(taskId))
						abstractionQueue.add(abstraction.getPredecessor());
				
				if (abstraction.getNeighbors() != null)
					for (D nb : abstraction.getNeighbors())
						if (nb.registerPathFlag(taskId))
							abstractionQueue.add(nb);
			}
		}
	}
	
	@Override
	public void computeTaintSources(final Set<AbstractionAtSink<D>> res) {
		if (res.isEmpty())
			return;
		
		long beforePathTracking = System.nanoTime();
		propagationCount = new AtomicInteger();
    	logger.info("Obtainted {} connections between sources and sinks", res.size());
    	
    	// Start the propagation tasks
    	int curResIdx = 0;
    	for (final AbstractionAtSink<D> abs : res) {
    		logger.info("Building path " + ++curResIdx);
    		executor.execute(new SourceFindingTask(lastTaskId++, abs, abs.getAbstraction()));
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
	
	@Override
	public void computeTaintPaths(final Set<AbstractionAtSink<D>> res) {
		System.err.println("WARNING: Path reconstruction is not supported");
		computeTaintSources(res);
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
