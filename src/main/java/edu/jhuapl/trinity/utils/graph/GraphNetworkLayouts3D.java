package edu.jhuapl.trinity.utils.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Graph-topology-only 3D layouts. Input edges are undirected for these methods. */
public final class GraphNetworkLayouts3D {
    private GraphNetworkLayouts3D() { }
    public static double[][] layout(int n, List<int[]> edges, GraphLayoutParams p) {
        List<List<Integer>> adj = new ArrayList<>(n);
        for (int i=0;i<n;i++) adj.add(new ArrayList<>());
        for (int[] e:edges) {
            if(e.length<2 || e[0]<0 || e[1]<0 || e[0]>=n || e[1]>=n || e[0]==e[1]) continue;
            adj.get(e[0]).add(e[1]); adj.get(e[1]).add(e[0]);
        }
        return switch(p.kind) {
            case FORCE_ATLAS_3D -> forceAtlas(adj,p);
            case KAMADA_KAWAI_3D -> stress(adj,p,true);
            case STRESS_MAJOR_3D -> stress(adj,p,false);
            case SPECTRAL_3D -> spectral(adj,p);
            default -> throw new IllegalArgumentException("Unsupported network layout: "+p.kind);
        };
    }
    private static double[][] seed(int n, double radius, long randomSeed) {
        double[][] pos=new double[n][3];
        Random rng=new Random(randomSeed);
        for(int i=0;i<n;i++) for(int d=0;d<3;d++) pos[i][d]=(rng.nextDouble()-.5)*radius;
        return pos;
    }
    /** Deterministic sampled repulsion + sparse edge attraction; avoids dense adjacency matrices. */
    private static double[][] forceAtlas(List<List<Integer>> adj, GraphLayoutParams p) {
        int n=adj.size(); double[][] pos=seed(n,Math.max(1,p.radius),p.randomSeed);
        if(n==0) return pos;
        double[][] delta=new double[n][3]; Random rng=new Random(p.randomSeed ^ 187L);
        int samples=Math.min(96,Math.max(0,n-1));
        int ticks=Math.max(1,p.iterations);
        for(int iter=0;iter<ticks;iter++) {
            if(Thread.currentThread().isInterrupted()) return pos;
            for(int i=0;i<n;i++) Arrays.fill(delta[i],0);
            for(int i=0;i<n;i++) {
                for(int j:adj.get(i)) if(j>i) {
                    double dist2=1e-5; for(int d=0;d<3;d++) {double x=pos[j][d]-pos[i][d];dist2+=x*x;}
                    double dist=Math.sqrt(dist2), attraction=Math.min(10,Math.max(.001,p.attraction)*dist);
                    for(int d=0;d<3;d++) {double f=attraction*(pos[j][d]-pos[i][d])/dist;delta[i][d]+=f;delta[j][d]-=f;}
                }
                for(int k=0;k<samples;k++) {
                    int j=rng.nextInt(n-1); if(j>=i) j++;
                    double dist2=1e-4; for(int d=0;d<3;d++) {double x=pos[i][d]-pos[j][d];dist2+=x*x;}
                    double rep=Math.min(50,Math.max(.001,p.repulsion)*.005/dist2);
                    for(int d=0;d<3;d++) delta[i][d]+=rep*(pos[i][d]-pos[j][d]);
                }
            }
            double rate=Math.max(.0001,p.step)*Math.pow(Math.min(.9999,Math.max(.01,p.cooling)),iter);
            for(int i=0;i<n;i++) for(int d=0;d<3;d++) pos[i][d]+=rate*(delta[i][d]-p.gravity*pos[i][d]);
        }
        normalize(pos,p.radius); return pos;
    }
    /** Landmark graph-distance stress; KK uses stronger inverse-square local weighting. */
    private static double[][] stress(List<List<Integer>> adj,GraphLayoutParams p,boolean kk) {
        int n=adj.size();double[][] pos=seed(n,Math.max(1,p.radius),p.randomSeed); if(n==0)return pos;
        int landmarks=Math.min(n,Math.max(1,p.landmarkCount));
        int[][] hops=new int[landmarks][];
        int[] anchors=new int[landmarks];
        for(int k=0;k<landmarks;k++) {anchors[k]=(int)((long)k*n/landmarks);hops[k]=bfs(adj,anchors[k]);}
        double[][] next=new double[n][3];
        int ticks=Math.max(1,p.iterations);
        for(int iter=0;iter<ticks;iter++) {
            if(Thread.currentThread().isInterrupted())return pos;
            double alpha=.35*Math.pow(.995,iter);
            for(int i=0;i<n;i++) {
                double[] f=new double[3];double mass=0;
                for(int k=0;k<landmarks;k++) {
                    int h=hops[k][i],j=anchors[k]; if(h<0 || i==j)continue;
                    double length=0;for(int d=0;d<3;d++) {double v=pos[i][d]-pos[j][d];length+=v*v;}
                    length=Math.sqrt(length)+1e-8;
                    double desired=Math.max(1,p.radius)*h/Math.max(2,Math.sqrt(n));
                    double w=kk?1.0/(h*h):1.0/Math.max(1,h);
                    for(int d=0;d<3;d++) f[d]+=w*(pos[j][d]+desired*(pos[i][d]-pos[j][d])/length);
                    mass+=w;
                }
                for(int d=0;d<3;d++)next[i][d]=mass>0?pos[i][d]*(1-alpha)+alpha*f[d]/mass:pos[i][d];
            }
            double[][] tmp=pos;pos=next;next=tmp;
        }
        normalize(pos,p.radius);return pos;
    }
    private static int[] bfs(List<List<Integer>> adj,int root) {
        int n=adj.size();int[] dist=new int[n],q=new int[n];Arrays.fill(dist,-1);
        int head=0,tail=0;q[tail++]=root;dist[root]=0;
        while(head<tail) {int u=q[head++];for(int v:adj.get(u))if(dist[v]<0){dist[v]=dist[u]+1;q[tail++]=v;}}
        return dist;
    }
    /** Three low-frequency normalized-adjacency modes, orthogonalized against constant vector. */
    private static double[][] spectral(List<List<Integer>> adj,GraphLayoutParams p) {
        int n=adj.size();double[][] pos=new double[n][3];if(n==0)return pos;
        Random random=new Random(p.randomSeed);
        double[][] axes=new double[3][n];
        double[] constant=new double[n];for(int i=0;i<n;i++)constant[i]=Math.sqrt(adj.get(i).size()+1.0);
        unit(constant);
        for(int axis=0;axis<3;axis++) {
            double[] v=axes[axis];for(int i=0;i<n;i++)v[i]=random.nextDouble()-.5;
            for(int it=0;it<Math.max(80,Math.min(400,p.iterations));it++) {
                if(Thread.currentThread().isInterrupted())return pos;
                double[] next=new double[n];
                for(int i=0;i<n;i++) {
                    double sum=v[i];for(int j:adj.get(i))sum+=v[j];
                    next[i]=sum/Math.sqrt((adj.get(i).size()+1.0));
                }
                for(int i=0;i<n;i++)next[i]/=Math.sqrt(adj.get(i).size()+1.0);
                orthogonalize(next,constant);
                for(int a=0;a<axis;a++)orthogonalize(next,axes[a]);
                unit(next);System.arraycopy(next,0,v,0,n);
            }
            for(int i=0;i<n;i++)pos[i][axis]=v[i];
        }
        normalize(pos,p.radius);return pos;
    }
    private static void orthogonalize(double[] v,double[] basis) {double dot=0;for(int i=0;i<v.length;i++)dot+=v[i]*basis[i];for(int i=0;i<v.length;i++)v[i]-=dot*basis[i];}
    private static void unit(double[] v){double sum=0;for(double x:v)sum+=x*x;double norm=Math.sqrt(sum);if(norm>1e-12)for(int i=0;i<v.length;i++)v[i]/=norm;}
    private static void normalize(double[][] pos,double radius){
        if(pos.length==0)return;double[] center=new double[3];for(double[] v:pos)for(int d=0;d<3;d++)center[d]+=v[d]/pos.length;
        double max=0;for(double[] v:pos)for(int d=0;d<3;d++){v[d]-=center[d];max=Math.max(max,Math.abs(v[d]));}
        if(max>1e-10)for(double[] v:pos)for(int d=0;d<3;d++)v[d]*=Math.max(1,radius)/max;
    }
}
