import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

public class SMTManager {
	Configuration config;
	LogManager logger;
	ShutdownNotifier notifier;
	
	SolverContext context;
	
	BooleanFormulaManager bmgr;
	IntegerFormulaManager imgr;
	
	public SMTManager() throws Exception{
		try {
			config = Configuration.defaultConfiguration();
		    logger = BasicLogManager.create(config);
		    notifier = ShutdownNotifier.createDummy();
		    
		    context = SolverContextFactory.createSolverContext(
		            config, logger, notifier, Solvers.SMTINTERPOL);
		    
		    FormulaManager fmgr = context.getFormulaManager();
	
		    bmgr = fmgr.getBooleanFormulaManager();
		    imgr = fmgr.getIntegerFormulaManager();
		    
		    
		} catch (Exception e) {
			throw new Exception("Invalid solver context: " + e.getMessage());
		}
	}
}
