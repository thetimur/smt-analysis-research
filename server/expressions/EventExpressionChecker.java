/**
 * EventExpressionChecker class documentation.
 *
 * This class is a EventExpressionChecker representation.
 *
 * IDebugDeviceNode -- Device tree implementation. Explains 
 * how devices are located relative to each other
 *
 * @author Garaev Timur
 */
public class EventExpressionChecker {
	/**
	 * parse validates that given expression makes sense in fixed setup 
     * @param expr when expression we want to check
	 * @param root root of current device's tree
     * @return true if event expression exists in current device setup
     */
	public Boolean parse(Expression expr, IDebugDeviceNode root) {
		Boolean result = true;
		if (expr instanceof VariableImpl) {
			result = parse((VariableImpl)expr, root);
		} else if (expr instanceof NotImpl) {
			result = parse(((NotImpl)expr).getExpression(), root);
		} else if (expr instanceof AndImpl) {
			result = parse(((AndImpl)expr).getLeft(), root) && parse(((AndImpl)expr).getRight(), root);
		} else if (expr instanceof OrImpl) {
			result = parse(((OrImpl)expr).getLeft(), root) && parse(((OrImpl)expr).getRight(), root);
		} else if (expr instanceof ComparisonImpl) {
			result = parse(((ComparisonImpl)expr).getLeft(), root) && parse(((ComparisonImpl)expr).getRight(), root);
		} else if (expr instanceof EqualityImpl) {
			result = parse(((EqualityImpl)expr).getLeft(), root) && parse(((EqualityImpl)expr).getRight(), root);
		}
		
		return result;
	}
	
	/**
	 * parse validates that given attribute usage makes sense in fixed setup 
     * @param expr variable we want to check
	 * @param root root of current device's tree
     * @return true if event variable exists in current device setup
     */
	private Boolean parse(VariableImpl expr, IDebugDeviceNode root) {
		List<IDebugDeviceNode> nodes = root.findChildrenByParentAndAttr(
				expr.getValue().getDevT().getName(),
				expr.getValue()
			);
		
		for (IDebugDeviceNode node : nodes) {
			if (node.findAllNodesByAttrScope(
					expr.getValue().getDevT().getName(),
					expr.getValue()
				).size() > 0) {
					return true;
				}
		}
		return false;
	}
}
