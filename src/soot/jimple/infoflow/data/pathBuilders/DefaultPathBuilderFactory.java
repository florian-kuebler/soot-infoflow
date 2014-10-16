package soot.jimple.infoflow.data.pathBuilders;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.data.ILinkedAbstraction;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

/**
 * Default factory class for abstraction path builders
 * 
 * @author Steven Arzt
 */
public class DefaultPathBuilderFactory<D extends ILinkedAbstraction<D>> implements IPathBuilderFactory<D> {
	
	/**
	 * Enumeration containing the supported path builders
	 */
	public enum PathBuilder {
		/**
		 * Simple context-insensitive, single-threaded, recursive approach to
		 * path reconstruction. Low overhead for small examples, but does not
		 * scale.
		 */
		Recursive,
		/**
		 * Highly precise context-sensitive path reconstruction approach. For
		 * a large number of paths or complex programs, it may be slow.
		 */
		ContextSensitive,
		/**
		 * A context-insensitive path reconstruction algorithm. It scales well,
		 * but may introduce false positives.
		 */
		ContextInsensitive,
		/**
		 * Very fast context-insensitive implementation that only finds
		 * source-to-sink connections, but no paths.
		 */
		ContextInsensitiveSourceFinder
	}
	
	private final PathBuilder pathBuilder;
	
	/**
	 * Creates a new instance of the {@link DefaultPathBuilderFactory} class
	 */
	public DefaultPathBuilderFactory() {
		this(PathBuilder.ContextSensitive);
	}

	/**
	 * Creates a new instance of the {@link DefaultPathBuilderFactory} class
	 * @param builder The path building algorithm to use
	 */
	public DefaultPathBuilderFactory(PathBuilder builder) {
		this.pathBuilder = builder;
	}
	
	@Override
	public IAbstractionPathBuilder<D> createPathBuilder(int maxThreadNum,
			BiDiInterproceduralCFG<Unit,SootMethod> icfg) {
		switch (pathBuilder) {
		case Recursive :
			return new RecursivePathBuilder<D>(icfg, maxThreadNum);
		case ContextSensitive :
			return new ContextSensitivePathBuilder<D>(icfg, maxThreadNum);
		case ContextInsensitive :
			return new ContextInsensitivePathBuilder<D>(icfg, maxThreadNum);
		case ContextInsensitiveSourceFinder :
			return new ContextInsensitiveSourceFinder<D>(maxThreadNum);
		}
		throw new RuntimeException("Unsupported path building algorithm");
	}
	
}
