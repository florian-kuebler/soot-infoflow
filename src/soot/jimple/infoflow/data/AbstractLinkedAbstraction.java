package soot.jimple.infoflow.data;

import java.util.BitSet;
import java.util.Collections;
import java.util.Set;

import com.google.common.collect.Sets;

import soot.jimple.Stmt;
import soot.jimple.infoflow.util.ConcurrentHashSet;

public abstract class AbstractLinkedAbstraction<D extends AbstractLinkedAbstraction<D>> implements ILinkedAbstraction<D> {

	protected D predecessor = null;
	protected Set<D> neighbors = null;
	protected Stmt currentStmt = null;
	protected Stmt correspondingCallSite = null;

	protected SourceContext sourceContext = null;

	// only used in path generation
	protected Set<SourceContextAndPath> pathCache = null;

	protected BitSet pathFlags = null;

	@Override
	public void setPredecessor(D predecessor) {
		this.predecessor = predecessor;
	}

	@Override
	public void addNeighbor(D originalAbstraction) {
		assert originalAbstraction.equals(this);

		// We should not register ourselves as a neighbor
		if (originalAbstraction == this)
			return;
		if (this.predecessor == originalAbstraction.predecessor && this.currentStmt == originalAbstraction.currentStmt)
			return;

		synchronized (this) {
			if (neighbors == null)
				neighbors = Sets.newIdentityHashSet();
			this.neighbors.add(originalAbstraction);
		}
	}

	@Override
	public void setCallingContext(D callingContext) {
		// TODO Auto-generated method stub

	}

	@Override
	public D getPredecessor() {
		return this.predecessor;
	}

	@Override
	public SourceContext getSourceContext() {
		return this.sourceContext;
	}

	@Override
	public Set<D> getNeighbors() {
		return this.neighbors;
	}

	@Override
	public Stmt getCurrentStmt() {
		return this.currentStmt;
	}

	@Override
	public Stmt getCorrespondingCallSite() {
		return this.correspondingCallSite;
	}

	@Override
	public boolean addPathElement(SourceContextAndPath scap) {
		if (this.pathCache == null) {
			synchronized (this) {
				if (this.pathCache == null) {
					this.pathCache = new ConcurrentHashSet<SourceContextAndPath>();
				}
			}
		}
		return this.pathCache.add(scap);
	}

	@Override
	public boolean registerPathFlag(int id) {
		if (pathFlags != null && pathFlags.get(id))
			return false;

		synchronized (this) {
			if (pathFlags == null)
				pathFlags = new BitSet();
			pathFlags.set(id);
		}
		return true;
	}

	@Override
	public Set<SourceContextAndPath> getPaths() {
		return pathCache == null ? null : Collections.unmodifiableSet(pathCache);
	}

}
