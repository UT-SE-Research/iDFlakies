package edu.illinois.cs.dt.tools.utility;

import scala.annotation.meta.getter;

public class Tuscan {
    public void main(int arg) {
        int n = arg;
        if (n == 3 || n == 5) {
            throw new RuntimeException("Impossible to solve for n=3 or n=5.");
        }
        generateTuscanSquare(n);
    }
    static int[][] r;
    static void helper(int[] a, int i) {
	System.arraycopy(a, 0, r[i], 0, a.length);
    }
    static void generateTuscanSquare(int n) {
    	int nn = n;
    	while((n-1) % 4 == 0 && n != 1 && n != 9) n = (n-1)/2+1;

	r = new int[nn][];
	for (int i = 0; i < nn; i++) {
	    r[i] = new int[nn+1];
	}

        if (n % 2 == 0) {
            // https://mathoverflow.net/questions/60856/hamilton-paths-in-k-2n/60859#60859
            int[] a = new int[n];
            for (int i = 0; i < n; i += 2) {
                a[i] = i / 2;
                a[i+1] = n - 1 - a[i];
            }
            helper(a, 0);
            for (int j = 1; j < n; j++) {
                for (int i = 0; i < n; i++) {
                    a[i] = (a[i] + 1) % n;
                }
                helper(a, j);
            }
        } else if (n % 4 == 3) {
            int k = (n - 3) / 4;
            int[] b = new int[n];
            for (int i = 0; i < n - 1; i++) {
                int p = (i == 0) ? 1 :
                        ((i == k + 1) ? 4*k + 2 :
                        ((i == 2*k + 2) ? 3 :
                        ((i == 3*k + 2) ? 4*k : 2*k)));
                int[] a = new int[n];
                for (int j = 0; j < n; j++) {
                    a[(j < p) ? n+j-p : j-p] = (j == 0) ? (n - 1) : (i + ((j % 2 == 0) ? (j / 2) : (n - 1 - (j - 1) / 2))) % (n - 1);
                }
                b[a[n-1]] = a[0];
                helper(a, i);
            }
            int[] t = new int[n];
            t[0] = n - 1;
            for (int i = 1; i < n; i++) {
                t[i] = b[t[i-1]];
            }
            helper(t, n-1);
        } else if (n == 9) {
            int[][] t = {{0,1,7,2,6,3,5,4,8},
                         {3,7,4,6,5,8,1,2,0},
                         {1,4,0,5,7,6,8,2,3},
                         {6,0,7,8,3,4,2,5,1},
                         {2,7,1,0,8,4,5,3,6},
                         {7,3,0,2,1,8,5,6,4},
                         {5,0,4,1,3,2,8,6,7},
                         {4,3,8,7,0,6,1,5,2},
                         {8,0,3,1,6,2,4,7,5}};
            for (int i = 0; i < 9; i++) {
                helper(t[i], i);
            }
        }
        else assert(false);

        while (nn != n){
	    // n + 1 == 4*m - 2
            // https://www.sciencedirect.com/science/article/pii/0095895680900441

	    n = n * 2 - 1;

            int h = (n + 1) / 2;
            
            for (int i = 0; i < h; i++) {
                for (int j = 0; j < h; j++) {
                    r[i][n-j] = r[i][j] + h;
                }
                // System.out.println(java.util.Arrays.toString(r[i]));
            }
            for (int i = h; i < n; i++) {
                /*
                for (int j = 0; j < n+1; j++) {
                    r[i][j] = ((j % 2 == 0) ? 0 : h) + (i-h + ((j % 2 == 0) ? (j / 2) : (n - (j - 1) / 2))) % h;
                }
                */
                for (int j = 0; j < h - 1; j++) {
                    r[i][j] = ((j % 2 == 0) ? 0 : h) + (i-h + ((j % 2 == 0) ? (j / 2) : (h - 2 - (j - 1) / 2))) % (h - 1);
                }
                r[i][h-1] = h-1;
                for (int j = h; j < n + 1; j++) {
                    r[i][j] = ((j % 2 == 0) ? 0 : h) + r[i][j-h] % h;
                }
                // System.out.println(java.util.Arrays.toString(r[i]));
            }
            for (int i = 0; i < n; i++) {
                int l = 0;
                for (; l < n; l++) {
                    if (r[i][l] == n) break;
                }
                int[] t = new int[n];
                System.arraycopy(r[i], l+1, t, 0, n-l);
                System.arraycopy(r[i], 0, t, n-l, l);

		System.arraycopy(t, 0, r[i], 0, n);
            }
        }

    //     for (int i = 0; i < nn; i++){
	//     System.out.print("[");
	//     for (int j = 0; j < nn; j++)
    //    		System.out.print((r[i][j]+ (j==nn-1?"":", ")));
	//     System.out.println("]");
	// }
    }

    public int[][] getMatrix(){
        return r;
    }
}
