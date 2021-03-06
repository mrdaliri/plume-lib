package plume;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*>>>
import org.checkerframework.checker.nullness.qual.*;
*/

/**
 * Graph utility methods. This class does not model a graph: all methods are static.
 *
 * @deprecated use org.plumelib.util.GraphPlume
 */
@Deprecated // use org.plumelib.util.GraphPlume
public final class GraphMDE {

  /** This class is a collection of methods; it does not represent anything. */
  private GraphMDE() {
    throw new Error("do not instantiate");
  }

  // Algorithms for computing dominators:
  //
  // Wikipedia:
  //  // dominator of the start node is the start itself
  //  Dom(n_0) = {n_0}
  //  // for all other nodes, set all nodes as the dominators
  //  for each n in N - {n_0}
  //      Dom(n) = N;
  //  // iteratively eliminate nodes that are not dominators
  //  while changes in any Dom(n)
  //      for each n in N - {n_0}:
  //          Dom(n) = {n} union with intersection over all p in pred(n) of Dom(p)
  //
  // Cooper/Harvey/Kennedy:
  //  for all nodes, n
  //    DOM[n] := {1 . . .N}
  //  Changed := true
  //  while (Changed)
  //    Changed := false
  //    for all nodes, n, in reverse postorder
  //      new_set := (Intersect_{p:=preds(n)} DOM[p]) union {n}
  //      if (new_set != DOM[n])
  //        DOM[n] := new_set
  //        Changed := true

  // The two algorithms are essentially the same; this implementation
  // follows the Wikipedia one.

  /**
   * Computes, for each node in the graph, its set of (pre-)dominators. Supply a successor graph if
   * you want post-dominators.
   *
   * @param <T> type of the graph nodes
   * @param predecessors a graph, represented as a predecessor map
   * @return a map from each node to a list of its pre-dominators
   */
  public static <T> Map<T, List<T>> dominators(Map<T, List</*@KeyFor("#1")*/ T>> predecessors) {

    // Map</*@KeyFor({"preds","dom"})*/ T,List</*@KeyFor({"preds","dom"})*/ T>> dom
    //   = new HashMap</*@KeyFor({"preds","dom"})*/ T,List</*@KeyFor({"preds","dom"})*/ T>>();
    Map<T, List<T>> dom = new HashMap<T, List<T>>();

    @SuppressWarnings("keyfor") // every element of pred's value will be a key for dom
    Map<T, List</*@KeyFor({"dom"})*/ T>> preds = predecessors;

    List<T> nodes = new ArrayList<T>(preds.keySet());

    // Compute roots & non-roots, for convenience
    List</*@KeyFor({"preds","dom"})*/ T> roots = new ArrayList<T>();
    List</*@KeyFor({"preds","dom"})*/ T> non_roots = new ArrayList<T>();

    // Initialize result:  for roots just the root, otherwise everything
    for (T node : preds.keySet()) {
      if (preds.get(node).isEmpty()) {
        // This is a root.  Its only dominator is itself.
        Set<T> set = Collections.singleton(node);
        dom.put(node, new ArrayList<T>(set));
        roots.add(node);
      } else {
        // Initially, set all nodes as dominators;
        // will later remove nodes that aren't dominators.
        dom.put(node, new ArrayList<T>(nodes));
        non_roots.add(node);
      }
    }
    assert roots.size() + non_roots.size() == nodes.size();

    // Invariants:
    // preds and dom have the same keyset.
    // All of the following are keys for both preds and dom:
    //  * every key in pred
    //  * elery element of every pred value
    //  * every key in dom
    //  * elery element of every dom value
    // So, the type of pred is now
    //
    // rather than its original type
    //   Map<T,List</*@KeyFor("preds")*/ T>> preds

    boolean changed = true;
    while (changed) {
      changed = false;
      for (T node : non_roots) {
        List<T> new_doms = null;
        assert preds.containsKey(node);
        for (T pred : preds.get(node)) {
          assert dom.containsKey(pred);
          /*@NonNull*/ List<T> dom_of_pred = dom.get(pred);
          if (new_doms == null) {
            // make copy because we may side-effect new_doms
            new_doms = new ArrayList<T>(dom_of_pred);
          } else {
            new_doms.retainAll(dom_of_pred);
          }
        }
        assert new_doms != null
            : "@AssumeAssertion(nullness): the loop was entered at least once because this is a non-root, which has at least one predecessor";
        new_doms.add(node);
        assert dom.containsKey(node);
        if (!dom.get(node).equals(new_doms)) {
          dom.put(node, new_doms);
          changed = true;
        }
      }
    }

    for (T node : preds.keySet()) {
      // TODO: The following two assert statements would be easier to read
      // than the one combined one, but a bug (TODO:  Jonathan will add a
      // bug number) prevents it from type-checking.
      // assert dom.containsKey(node);
      // assert dom.get(node).contains(node);
      assert dom.containsKey(node) && dom.get(node).contains(node);
    }

    return dom;
  }

  /**
   * Print a representation of the graph to ps, indented by intent spaces.
   *
   * @param <T> the type of nodes of the graph
   * @param graph the graph to print
   * @param ps the PrintStream to which to print the graph
   * @param indent the number of spaces by which to indent the printed representation
   */
  public static <T> void print(Map<T, List<T>> graph, PrintStream ps, int indent) {
    String indentString = "";
    for (int i = 0; i < indent; i++) {
      indentString += " ";
    }
    for (T node : graph.keySet()) {
      ps.printf("%s%s%n", indentString, node);
      for (T child : graph.get(node)) {
        ps.printf("  %s%s%n", indentString, child);
      }
    }
  }
}
