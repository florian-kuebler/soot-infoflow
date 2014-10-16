package soot.jimple.infoflow.data.pathBuilders;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.data.ILinkedAbstraction;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

/**
 * Abstract base class for all abstraction path builders
 * 
 * @author Steven Arzt
 */
public abstract class AbstractAbstractionPathBuilder<D extends ILinkedAbstraction<D>> implements
		IAbstractionPathBuilder<D> {
	protected final BiDiInterproceduralCFG<Unit,SootMethod> icfg;
	
	public AbstractAbstractionPathBuilder(BiDiInterproceduralCFG<Unit,SootMethod> icfg) {
		this.icfg = icfg;
	}

}
