package edu.jhuapl.trinity.utils.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Deterministic topology-driven layouts; original graph edges are not modified. */
public final class GraphTopologyLayouts3D {
    private GraphTopologyLayouts3D() { }

    /** Edges are node-index pairs. A directed spanning forest is derived from them. */
    public static double[][] layout(int n, List<int[]> edges, GraphLayoutParams params) {
        double[][] xyz = new double[n][3];
        if (n == 0) return xyz;
        List<List<Integer>> out = new ArrayList<>(n);
        int[] indegree = new int[n];
        for (int i=0;i<n;i++) out.add(new ArrayList<>());
        for (int[] e: edges) {
            if(e[0]<0 || e[1]<0 || e[0]>=n || e[1]>=n || e[0]==e[1]) continue;
            out.get(e[0]).add(e[1]); indegree[e[1]]++;
        }
        for (List<Integer> neighbors:out) neighbors.sort(Integer::compare);
        Integer[] order = new Integer[n];
        for (int i=0;i<n;i++) order[i]=i;
        Arrays.sort(order, Comparator.comparingInt((Integer i)->indegree[i]).thenComparingInt(i->i));
        int[] parent=new int[n], depth=new int[n];
        Arrays.fill(parent,-1);
        boolean[] seen=new boolean[n];
        List<Integer> roots=new ArrayList<>();
        ArrayDeque<Integer> queue=new ArrayDeque<>();
        for(int root:order) {
            if(seen[root]) continue;
            roots.add(root); seen[root]=true; queue.add(root);
            while(!queue.isEmpty()) {
                int u=queue.removeFirst();
                for(int v:out.get(u)) if(!seen[v]) {
                    seen[v]=true; parent[v]=u; depth[v]=depth[u]+1; queue.addLast(v);
                }
            }
        }
        List<List<Integer>> children=new ArrayList<>(n);
        for(int i=0;i<n;i++) children.add(new ArrayList<>());
        for(int i=0;i<n;i++) if(parent[i]>=0) children.get(parent[i]).add(i);
        int[] size=new int[n];
        for(int root:roots) subtree(root,children,size);
        double radius=Math.max(1.0,params.radius);
        double span=2*Math.PI/roots.size();
        for(int i=0;i<roots.size();i++) {
            double lo=i*span, hi=(i+1)*span;
            place(roots.get(i),lo,hi,depth,children,size,xyz,params.kind,radius);
        }
        return xyz;
    }
    private static int subtree(int u,List<List<Integer>> children,int[] size) {
        int s=1;
        for(int v:children.get(u)) s+=subtree(v,children,size);
        size[u]=s; return s;
    }
    private static void place(int u,double lo,double hi,int[] depth,List<List<Integer>> children,
                              int[] size,double[][] xyz,GraphLayoutParams.LayoutKind kind,double radius) {
        double a=(lo+hi)/2, d=depth[u]+1;
        if(kind==GraphLayoutParams.LayoutKind.LAYERED_HIERARCHY_3D) {
            xyz[u]=new double[]{radius*Math.cos(a),d*radius*0.45,radius*Math.sin(a)};
        } else if(kind==GraphLayoutParams.LayoutKind.HYPERBOLIC_HIERARCHY_3D) {
            // Poincare ball projection: tanh(hyperbolic radius / 2).
            double rho=Math.tanh(d*0.72/2.0)*radius;
            double elevation=0.35*Math.sin(a*1.7);
            xyz[u]=new double[]{rho*Math.cos(a)*Math.cos(elevation),rho*Math.sin(elevation),rho*Math.sin(a)*Math.cos(elevation)};
        } else {
            double r=d*radius*0.38;
            xyz[u]=new double[]{r*Math.cos(a),r*Math.sin(a),d*radius*0.12};
        }
        int total=0;
        for(int v:children.get(u)) total+=size[v];
        double cursor=lo;
        for(int v:children.get(u)) {
            double next=cursor+(hi-lo)*size[v]/Math.max(1,total);
            place(v,cursor,next,depth,children,size,xyz,kind,radius);
            cursor=next;
        }
    }
}
