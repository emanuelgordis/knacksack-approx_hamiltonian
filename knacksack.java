package diver;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;

import graph.FindState;
import graph.FleeState;
import graph.Node;
import graph.NodeStatus;
import graph.SewerDiver;

public class McDiver extends SewerDiver {

    /** Find the ring in as few steps as possible. Once you get there, <br>
     * you must return from this function in order to pick<br>
     * it up. If you continue to move after finding the ring rather <br>
     * than returning, it will not count.<br>
     * If you return from this function while not standing on top of the ring, <br>
     * it will count as a failure.
     *
     * There is no limit to how many steps you can take, but you will receive<br>
     * a score bonus multiplier for finding the ring in fewer steps.
     *
     * At every step, you know only your current tile's ID and the ID of all<br>
     * open neighbor tiles, as well as the distance to the ring at each of <br>
     * these tiles (ignoring walls and obstacles).
     *
     * In order to get information about the current state, use functions<br>
     * currentLocation(), neighbors(), and distanceToRing() in state.<br>
     * You know you are standing on the ring when distanceToRing() is 0.
     *
     * Use function moveTo(long id) in state to move to a neighboring<br>
     * tile by its ID. Doing this will change state to reflect your new position.
     *
     * A suggested first implementation that will always find the ring, but <br>
     * likely won't receive a large bonus multiplier, is a depth-first walk. <br>
     * Some modification is necessary to make the search better, in general. */
    public HashSet<Long> visitedNodes;

    public McDiver() {
        visitedNodes= new HashSet<>();
    }

    @Override
    public void find(FindState state) {
        // TODO : Find the ring and return.
        // DO NOT WRITE ALL THE CODE HERE. DO NOT MAKE THIS METHOD RECURSIVE.
        // Instead, write your method (it may be recursive) elsewhere, with a
        // good specification, and call it from this one.
        //
        // Working this way provides you with flexibility. For example, write
        // one basic method, which always works. Then, make a method that is a
        // copy of the first one and try to optimize in that second one.
        // If you don't succeed, you can always use the first one.
        //

        dfs(state);

    }

    /*Compares the distance to the ring of two nodes.
     *Returns negative integer if they have the same distance to ring or other node has a smaller ID number
     *Returns positive integer if one node has greater distance to ring
     */
    public int compareTo(NodeStatus one, NodeStatus other) {
        // if (distance != other.distance) return Integer.compare(distance, other.distance);
        if (one.getDistanceToRing() == other.getDistanceToRing())
            return Long.compare(-one.getId(), other.getId());
        return one.getDistanceToRing() - other.getDistanceToRing();
    }

    /** The walker is standing on a Node current (say) given by FindState state. Visit every node
     * reachable along paths of unvisited nodes from node current. End with walker standing on Node
     * current. Precondition: current is unvisited. */

    public void dfs(FindState state) {
        NodeStatus[] path= state.neighbors().toArray(new NodeStatus[state.neighbors().size()]);
        if (path.length > 1) {
            Arrays.sort(path, (n1, n2) -> compareTo(n1, n2));
        }
        Long current= state.currentLocation();
        visitedNodes.add(current);
        if (state.distanceToRing() == 0) return;
        for (NodeStatus n : path) {

            if (!visitedNodes.contains(n.getId())) {
                state.moveTo(n.getId());

                dfs(state);
                if (state.distanceToRing() == 0) return;
                state.moveTo(current);
            }
        }

    }

    /** Flee --get out of the sewer system before the steps are all used, trying to <br>
     * collect as many coins as possible along the way. McDiver must ALWAYS <br>
     * get out before the steps are all used, and this should be prioritized above<br>
     * collecting coins.
     *
     * You now have access to the entire underlying graph, which can be accessed<br>
     * through FleeState. currentNode() and exit() will return Node objects<br>
     * of interest, and getNodes() will return a collection of all nodes on the graph.
     *
     * You have to get out of the sewer system in the number of steps given by<br>
     * stepToGo(); for each move along an edge, this number is <br>
     * decremented by the weight of the edge taken.
     *
     * Use moveTo(n) to move to a node n that is adjacent to the current node.<br>
     * When n is moved-to, coins on node n are automatically picked up.
     *
     * You must return from this function while standing at the exit. Failing <br>
     * to do so before steps run out or returning from the wrong node will be<br>
     * considered a failed run.
     *
     * Initially, there are enough steps to get from the starting point to the<br>
     * exit using the shortest path, although this will not collect many coins.<br>
     * For this reason, a good starting solution is to use the shortest path to<br>
     * the exit. */
    @Override
    public void flee(FleeState state) {
        // TODO: Get out of the sewer system before the steps are used up.
        // DO NOT WRITE ALL THE CODE HERE. Instead, write your method elsewhere,
        // with a good specification, and call it from this one.

        flightHelper(state);
        greedyFlight2(state);

    }

    /** A collection of all nodes. */
    Collection<Node> nodes;

    /** Map of every node fewer steps away from the ring than steps allowed <br>
     * to a PathInfo object containing the distance and shortest path to exit. */
    HashMap<Node, PathInfo> allPathsToExit;

    /** Populate nodes and allPathsToExit. */
    public void flightHelper(FleeState state) {
        nodes= state.allNodes();
        allPathsToExit= new HashMap<>();

        for (Node n : nodes) {

            if (n.getTile().coins() != 0) {
                LinkedList<Node> listToExit= (LinkedList<Node>) A6.shortest(n, state.exit());
                Object[] pathToExit= listToExit.toArray();
                int distanceToExit= 0;

                for (int j= 0; j < pathToExit.length - 1; j++ ) {
                    Node i= (Node) pathToExit[j];
                    distanceToExit+= i.getEdge((Node) pathToExit[j + 1]).length;
                }
                if (distanceToExit <= state.stepsToGo()) {
                    allPathsToExit.put(n, new PathInfo(listToExit, distanceToExit));
                }
            }
        }
    }

    /** Move to the exit while collecting as many coins as possible with the number of steps
     * remaining. <br>
     * Nodes with coins are ranked by their value per distance. If no nodes with coins are
     * reachable, <br>
     * head straight to exit. */
    public void greedyFlight2(FleeState state) {
        Node current= state.currentNode();

        Heap<Node> allNodes= new Heap<>(false);
        HashMap<Node, PathInfo> allPaths= new HashMap<>();
        for (Node n : allPathsToExit.keySet()) {
            int value= n.getTile().coins();
            if (value != 0) {
                LinkedList<Node> listToN= (LinkedList<Node>) A6.shortest(current, n);
                Object[] pathToN= listToN.toArray();
                int distance= 0;
                for (int j= 0; j < pathToN.length - 1; j++ ) {
                    Node i= (Node) pathToN[j];
                    distance+= i.getEdge((Node) pathToN[j + 1]).length;
                    value+= i.getTile().coins();
                }

                if (distance + allPathsToExit.get(n).dist < state.stepsToGo()) {
                    allNodes.insert(n, 15 * value / distance);
                    allPaths.put(n, new PathInfo(listToN, distance));

                }
            }

        }

        while (allNodes.size > 0) {

            Node f= allNodes.poll();
            ListIterator<Node> listIter= allPaths.get(f).path.listIterator(1);

            while (listIter.hasNext()) {
                Node n= listIter.next();
                if (state.currentNode().equals(state.exit())) return;
                state.moveTo(n);
                greedyFlight2(state);

            }
        }

        if (state.currentNode().equals(state.exit())) return;

        LinkedList<Node> pathToExit;
        if (allPathsToExit.containsKey(current)) {
            pathToExit= allPathsToExit.get(current).path;
        } else {
            pathToExit= (LinkedList<Node>) A6.shortest(current, state.exit());
        }
        ListIterator<Node> listIter= pathToExit.listIterator(1);
        while (listIter.hasNext()) {
            Node n= listIter.next();

            state.moveTo(n);

        }

    }

    /** An instance contains information about a node: <br>
     * the Distance of this node from the current node and <br>
     * its the shortest path between them. */
    public static class PathInfo {
        /** shortest known distance from the current location to this node. */
        private int dist;
        /** shortest path from the current location to this node. */
        private LinkedList<Node> path;

        /** Constructor: an instance with distToExit d to this node and<br>
         * shortest path p. */
        public PathInfo(LinkedList<Node> p, int d) {
            dist= d;
            path= p;
        }

    }

//    public void shortestFlight(FleeState state) {
//        List<Node> path= A6.shortest(state.currentNode(), state.exit());
//        for (Node n : path) {
//            if (n != state.currentNode()) {
//                state.moveTo(n);
//            }
//        }
//
//    }

//    public void greedyFlight(FleeState state) {
//        if (state.currentNode().equals(state.exit())) return;
//
//        Node[] allNodes= state.allNodes().toArray(new Node[state.allNodes().size()]);
//
//        Arrays.sort(allNodes,
//            (n1, n2) -> Integer.compare(
//                16 * n1.getTile().coins() / A6.shortest(state.currentNode(), n1).size(),
//                16 * n2.getTile().coins() / A6.shortest(state.currentNode(), n2).size()));
//        for (int i= allNodes.length - 1; i > -1; i-- ) {
//            if (allNodes[i].getTile().coins() == 0) break;
//            List<Node> path1= A6.shortest(state.currentNode(), allNodes[i]);
//            List<Node> path2= A6.shortest(allNodes[i], state.exit());
//            int sum1= 0;
//            for (int j= 0; j < path1.size() - 1; j++ ) {
//                sum1+= path1.get(j).getEdge(path1.get(j + 1)).length;
//            }
//
//            int sum2= 0;
//            for (int j= 0; j < path2.size() - 1; j++ ) {
//                sum2+= path2.get(j).getEdge(path2.get(j + 1)).length;
//            }
//
//            if (sum1 + sum2 < state.stepsToGo()) {
//                for (Node n : path1) {
//                    if (n != state.currentNode()) {
//                        state.moveTo(n);
//                    }
//                }
//                greedyFlight(state);
//
//            }
//        }
//
//        List<Node> pathToExit= A6.shortest(state.currentNode(), state.exit());
//        pathToExit.remove(0);
//        for (Node n : pathToExit) {
//            state.moveTo(n);
//        }
//
//    }

}
