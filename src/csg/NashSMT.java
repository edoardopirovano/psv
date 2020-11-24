package csg;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import com.microsoft.z3.*;

import explicit.Distribution;

public class NashSMT {
	
	private RealExpr[] payvars;
	private ArithExpr[] payoffs;

	private RealExpr[] vars;
	private RealExpr[] p1vars;
	private RealExpr[] p2vars;
	private IntExpr zero;
	private IntExpr one;
	private BoolExpr[] xlabels;
	private BoolExpr[] ylabels;
	private ArithExpr[] xexps;
	private ArithExpr[] yexps;
	private BoolExpr eq;
	private int nrows = 0;
	private int ncols = 0;
	private HashMap<String,ArrayList<Double>> eqs;
	private int neq = 0;
	private double[] p1p;
	private double[] p2p;
	private double[][] a;
	private double[][] b;
    
	private String[] lvp1;
    private String[] lvp2;
    
    private HashMap<String, String> cfg;
    private Context ctx;
    private Solver s;
    
    private RealExpr cp1;
    private RealExpr cp2;
    private double time;
    
    private Optimize opt;
    
    private ArrayList<ArrayList<Distribution>> strat;
    
    public NashSMT() {
    	cfg = new HashMap<String, String>();
        cfg.put("model", "true");
        ctx = new Context(cfg);
        s = ctx.mkSolver();        
        opt = ctx.mkOptimize();
    }
    
    public NashSMT(int nrows, int ncols, double[][] a, double[][] b) {
    	cfg = new HashMap<String, String>();
        cfg.put("model", "true");
        ctx = new Context(cfg);
        s = ctx.mkSolver();
        this.nrows = nrows;
        this.ncols = ncols;
        this.a = a;
        this.b = b;
        opt = ctx.mkOptimize();
        initialize();
    }
        
    public void update(int nrows, int ncols, double[][] a, double[][] b) {
        s.reset();
        this.nrows = nrows;
        this.ncols = ncols;
        this.a = a;
        this.b = b;
        initialize();
    }
    
	private void initialize() {
		zero = ctx.mkInt(0);
	    one = ctx.mkInt(1);
		vars = new RealExpr[nrows+ncols];
		p1vars = new RealExpr[nrows];
		p2vars = new RealExpr[ncols];
		lvp1 = new String[nrows];
		lvp2 = new String[ncols];
		strat = new ArrayList<ArrayList<Distribution>>();
		int i = 0, j = 0;
		for(; i < nrows; i++) {
			vars[i] = ctx.mkRealConst("x"+i);
			p1vars[i] = vars[i];
			lvp1[i] = "x"+i;
		}
		for(; j < ncols; j++) {
			vars[i] = ctx.mkRealConst("y"+j);
			p2vars[j] = vars[i];
			lvp2[j] = "y"+j;
			i++;
		}
		for(RealExpr v : vars) {
			s.add(ctx.mkLe(v, one));
			s.add(ctx.mkGe(v, zero));
		}
	}
	
	private void xLabels() {
		BoolExpr[] tmp = new BoolExpr[ncols-1];
		int l = 0;
		int j = 0;
		xlabels = new BoolExpr[nrows+ncols];
		for(int i = 0; i < nrows+ncols; i++) {
			if(i < nrows) {
				xlabels[i] = ctx.mkEq(vars[i], zero);
			}
			else {
				for(int k = 0; k < ncols; k++) {
					if(j != k) {
						tmp[l] = ctx.mkGe(xexps[j], xexps[k]);
						l++;
					}
				}
				xlabels[i] = tmp[0];
				if(tmp.length > 1) {
					for(int m = 1; m < tmp.length; m++) {
						xlabels[i] = ctx.mkAnd(xlabels[i], tmp[m]);
					}
				}
				j++;
				l = 0;
			}
		}
	}
	
	private void yLabels() {
		BoolExpr[] tmp = new BoolExpr[nrows-1];
		int l = 0;
		int j = 0;
		ylabels = new BoolExpr[nrows+ncols];
		for(int i = 0; i < nrows+ncols; i++) {
			if(i < nrows) {
				for(int k = 0; k < nrows; k++) {
					if(j != k) {
						tmp[l] = ctx.mkGe(yexps[j], yexps[k]);
						l++;
					}
				}
				ylabels[i] = tmp[0];
				if(tmp.length > 1) {
					for(int m = 1; m < tmp.length; m++) {
						ylabels[i] = ctx.mkAnd(ylabels[i], tmp[m]);
					}
				}
				j++;
				l = 0;
			}
			else {
				ylabels[i] = ctx.mkEq(vars[i], zero);
			}
		}
	}
	
	private void vMult() {
		yexps = new ArithExpr[nrows];
		xexps = new ArithExpr[ncols];
		ArithExpr curr;
		for(int i = 0; i < nrows; i++) {
			curr = zero;
			for(int j = 0; j < ncols; j++) {
				try {
					curr = ctx.mkAdd(curr, ctx.mkMul(vars[nrows+j], ctx.mkReal(String.valueOf(a[i][j]))));
				}
				catch(Exception e) {
					System.out.println(a[i][j]);
					System.out.println(String.valueOf(a[i][j]));
					e.printStackTrace();
				}
			}
			yexps[i] = curr;
		}
		for(int j = 0; j < ncols; j++) {
			curr = zero;
			for(int i = 0; i < nrows; i++) {
				curr = ctx.mkAdd(curr, ctx.mkMul(vars[i], ctx.mkReal(String.valueOf(b[i][j]))));
			}
			xexps[j] = curr;
		}
	}
	
	public void compEq() {
		vMult();
		xLabels();
		yLabels();
		int i,j;
		eq = ctx.mkTrue();
		for(i = 0; i < nrows+ncols; i++) {
			eq = ctx.mkAnd(eq, ctx.mkOr(xlabels[i],ylabels[i]));
		}
		Expr xctr = zero;
		Expr yctr = zero;
		for(i = 0; i < nrows; i++) {
			xctr = ctx.mkAdd((ArithExpr) xctr, vars[i]);
		}
		xctr = ctx.mkEq(xctr, one);
		for(j = i; j < nrows+ncols; j++) {
			yctr = ctx.mkAdd((ArithExpr) yctr, vars[j]);
		}
		yctr = ctx.mkEq(yctr, one);		
		s.add((BoolExpr) xctr); 
		s.add((BoolExpr) yctr); 
		s.add(eq);
		
        Model model = null;                
        BoolExpr rstr;
        BoolExpr prstr;
        j = 0; 
        eqs = new HashMap<String,ArrayList<Double>> ();
        long start = new Date().getTime();

        //in order to check for indifference
        BoolExpr xeqrst = ctx.mkTrue();
        BoolExpr yeqrst = ctx.mkTrue();

        /*
        for(int nv = 0; nv < p1vars.length - 1; nv++) {
        	xeqrst = ctx.mkAnd(xeqrst, ctx.mkEq(p1vars[nv], p1vars[nv+1]));
        }
        
        for(int nv = 0; nv < p2vars.length - 1; nv++) {
        	yeqrst = ctx.mkAnd(yeqrst, ctx.mkEq(p2vars[nv], p2vars[nv+1]));
        }
        */
       
        /*** Too strong? ***/
       
       /* 
        for(int nv = 0; nv < p1vars.length; nv++) {
            for(int nnv = 0; nnv < p1vars.length; nnv++) {
            	xeqrst = ctx.mkAnd(xeqrst, 
            					   ctx.mkOr(
            							   	ctx.mkNot(ctx.mkEq(p1vars[nv], p1vars[nnv])),
            							   	ctx.mkAnd(ctx.mkEq(p1vars[nv], zero),
            							   			  ctx.mkEq(p1vars[nnv], zero))	
            							   )
            						);
            }
        }
        
        for(int nv = 0; nv < p2vars.length; nv++) {
            for(int nnv = 0; nnv < p2vars.length; nnv++) {
            	xeqrst = ctx.mkAnd(xeqrst, 
            					   ctx.mkOr(
            							    ctx.mkNot(ctx.mkEq(p2vars[nv], p2vars[nnv])),
            							    ctx.mkAnd(ctx.mkEq(p2vars[nv], zero),
            							    		  ctx.mkEq(p2vars[nnv], zero))	
            							   )
            						);
            }
        }
        */
        
        
        /*** Too strong? ***/
        
        xeqrst = ctx.mkNot(xeqrst);
        yeqrst = ctx.mkNot(yeqrst);
        
        boolean indif = false;
        
        addPayoffs();
                
        /*
        Optimize opt = ctx.mkOptimize();
        opt.Add(ctx.mkEq(payoffs[0],  payvars[0]));
        opt.Add(ctx.mkEq(payoffs[1],  payvars[1]));
        opt.MkMaximize(payvars[0]);
        Optimize.Handle mx = opt.MkMaximize(payvars[0]);
        Optimize.Handle my = opt.MkMaximize(payvars[1]);
        */
        
        BoolExpr c1;
        BoolExpr c2;
        
        BoolExpr p1;
        BoolExpr p2;        
        
        strat = new ArrayList<ArrayList<Distribution>>();
        ArrayList<Distribution> dists;
        Distribution dist1;
        Distribution dist2;
        double p;
        
        while (Status.SATISFIABLE == s.check()) {
            model = s.getModel();
            rstr = ctx.mkFalse();

            c1 = ctx.mkTrue();
            c2 = ctx.mkTrue();
                     
            /*
            if(j > 0) {          	
            	if(((BoolExpr) model.eval(xeqrst, true)).isFalse()) {
            		System.out.println("## Player 1 is indifferent. ##");
            		indif = true;
            		//break;
            	}
            	if(((BoolExpr) model.eval(yeqrst, true)).isFalse()) {
            		System.out.println("## Player 2 is indifferent. ##");
            		indif = true;
            		//break;	
            	}      	            	
            }
            */
            
            //System.out.println(rat2double((RatNum) model.getConstInterp(payvars[0])));
            //System.out.println(rat2double((RatNum) model.getConstInterp(payvars[1])));
                        
            //compPayoffs(model);
                        
           //strat.clear();
            
            dists = new ArrayList<Distribution>();
            dist1 = new Distribution();
            dist2 = new Distribution();
            
            for(i = 0; i < vars.length; i++) {
            		p = rat2double(((RatNum) model.getConstInterp(vars[i])));
            		if(p > 0) {
            			if(i < nrows)
            				dist1.add(i, p);
            			else 
            				dist2.add(i-nrows, p);
            		}
            		if(j == 0) {
            			eqs.put(vars[i].getSExpr(), new ArrayList<Double> ());
            			eqs.get(vars[i].getSExpr()).add(p);
            		}
            		else {
            			//System.out.println((RatNum) model.getConstInterp(vars[i]));
            			eqs.get(vars[i].getSExpr().toString()).add(p);	
            		}
            	//rstr = ctx.mkOr(rstr, ctx.mkNot(ctx.mkEq(vars[i], (RealExpr) model.getConstInterp(vars[i]))));
            }
            
            for(i = 0; i < p1vars.length; i++) {
            		if(((BoolExpr) model.eval(ctx.mkEq(p1vars[i], zero), true)).isFalse()) {
            			c1 = ctx.mkAnd(c1, ctx.mkNot(ctx.mkEq(p1vars[i], zero)));
            		}
            		else {
            			c1 = ctx.mkAnd(c1, ctx.mkEq(p1vars[i], zero));
            		}
            }
            
            for(i = 0; i < p2vars.length; i++) {
            		if(((BoolExpr) model.eval(ctx.mkEq(p2vars[i], zero), true)).isFalse()) {
            			c2 = ctx.mkAnd(c2, ctx.mkNot(ctx.mkEq(p2vars[i], zero)));
            		}
            		else {
            			c2 = ctx.mkAnd(c2, ctx.mkEq(p2vars[i], zero));
            		}
            }            
            
            /*
            p1 = ctx.mkAnd(c2, ctx.mkEq(payvars[0], model.getConstInterp(payvars[0])));
            p2 = ctx.mkAnd(c1, ctx.mkEq(payvars[1], model.getConstInterp(payvars[1])));
            */

            /*
        		prstr = ctx.mkAnd(ctx.mkEq(payvars[0], model.getConstInterp(payvars[0])),
        					  	  ctx.mkEq(payvars[1], model.getConstInterp(payvars[1]))
        					 	);
            */
            
            //System.out.println(opt.Check()); 
            //System.out.println(mx.getValue());
            //System.out.println(my.getValue());
            
            if(indif) {
            		j++;
            		break;
            }
            /*
			System.out.println("--");
			for(String v : eqs.keySet()) {
				System.out.println(v + " " + eqs.get(v).get(j));
			}
			System.out.println("--");
            */
            //System.out.println("$$ dist1 " + dist1);
            //System.out.println("$$ dist2 " + dist2);
            dists.add(0,dist1);
            dists.add(1,dist2);
            strat.add(j,dists);
    		j++;
    		s.add(ctx.mkOr(ctx.mkNot(c1),ctx.mkNot(c2)));
    		//s.add(rstr);
            //s.add(ctx.mkNot(p2));
            //s.add(ctx.mkNot(p1));
        }
        long end = new Date().getTime();
        time = (end-start)/1000.000;
        //System.out.println("Time: " + time); 
        if(!indif) {
        		//System.out.println(j + " equilibrium(a)");
        }
        else {
        	//System.out.println(j-1 + " equilibrium(a)");
        	//System.out.println("## One of the players is indifferent. ##");
        }
        neq = j;
	}
		
	public void addPayoffs() {
		payvars = new RealExpr[2];
		payoffs = new ArithExpr[2];
		payvars[0] = ctx.mkRealConst("pp1");
		payvars[1] = ctx.mkRealConst("pp2");
		payoffs[0] = ctx.mkInt(0);
		payoffs[1] = ctx.mkInt(0);
		for(int i = 0; i < nrows; i++) {
			for(int j = 0; j < ncols; j++) {
				payoffs[0] = ctx.mkAdd(payoffs[0], ctx.mkMul(p1vars[i], p2vars[j], ctx.mkReal(String.valueOf(a[i][j]))));
				payoffs[1] = ctx.mkAdd(payoffs[1], ctx.mkMul(p1vars[i], p2vars[j], ctx.mkReal(String.valueOf(b[i][j]))));
			}
		}
		s.add(ctx.mkEq(payvars[0], payoffs[0]));
		s.add(ctx.mkEq(payvars[1], payoffs[1]));
	}	

	public void compPayoffs(Model model) {	
		double p1p = 0.0;
		double p2p = 0.0;
        for(int i = 0; i < nrows; i++) {
        	for(int j = 0; j < ncols; j++) {
        		p1p += rat2double(((RatNum) model.getConstInterp(p1vars[i])))*
        			   rat2double(((RatNum) model.getConstInterp(p2vars[j])))*a[i][j];
        		p2p += rat2double(((RatNum) model.getConstInterp(p1vars[i])))*
        			   rat2double(((RatNum) model.getConstInterp(p2vars[j])))*b[i][j];
        	}
        }	
        cp1 = ctx.mkReal(String.valueOf(p1p));
        cp2 = ctx.mkReal(String.valueOf(p2p));
	}
	
	public void compPayoffs() {	
		p1p = new double[neq];
		p2p = new double[neq];
		Arrays.fill(p1p, 0.0);
		Arrays.fill(p2p, 0.0);
		for(int e = 0; e < neq; e++) {
        		for(int i = 0; i < nrows; i++) {
        			for(int j = 0; j < ncols; j++) {
        				p1p[e] += eqs.get(p1vars[i].toString()).get(e)*eqs.get(p2vars[j].toString()).get(e)*a[i][j];
        				p2p[e] += eqs.get(p1vars[i].toString()).get(e)*eqs.get(p2vars[j].toString()).get(e)*b[i][j];
        			}
        		}
        }		
	}
	
	public void print() {
		//System.out.println("--");
			for(int i = 0; i < neq; i++) {
				for(String v : eqs.keySet()) {
					//System.out.println(v + " " + eqs.get(v).get(i));
				}
				//System.out.println("p1 " + p1p[i]);
				//System.out.println("p2 " + p2p[i]);
				//System.out.println("--");
        }
	}
	
	public RealExpr[] getP1vars() {
		return p1vars;
	}

	public RealExpr[] getP2vars() {
		return p2vars;
	}

	public double[] getP1p() {
		return p1p;
	}

	public double[] getP2p() {
		return p2p;
	}
	
	public int getNeq() {
		return neq;
	}

	public ArrayList<ArrayList<Distribution>> getStrat() {
		return strat;
	}
	
	public HashMap<String, ArrayList<Double>> getEqs() {
		return eqs;
	}

	public String[] getLvp1() {
		return lvp1;
	}

	public String[] getLvp2() {
		return lvp2;
	}

	private double rat2double(RatNum n) {
		double num = n.getNumerator().getBigInteger().doubleValue();
		double den = n.getDenominator().getBigInteger().doubleValue();
		return num/den;
	}
	
}
