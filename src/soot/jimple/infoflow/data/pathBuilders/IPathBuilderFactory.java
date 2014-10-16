package soot.jimple.infoflow.data.pathBuilders;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.data.ILinkedAbstraction;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;


/**
 * Common interface for all path builder factories
 * 
 * @author Steven Arzt
 */
public interface IPathBuilderFactory<D extends ILinkedAbstraction<D>> {
	
	/**
	 * Creates a new path builder
	 * @param maxThreadNum The maximum number of threads to use
	 * @param icfg The interprocedural CFG to use
	 * @return The newly created path builder
	 */
	public IAbstractionPathBuilder<D> createPathBuilder
			(int maxThreadNum, BiDiInterproceduralCFG<Unit,SootMethod> icfg);

}
