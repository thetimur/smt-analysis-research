public class TransitionStateMachine {
	public TransitionStateMachine() {
		idToIndex = new HashMap<>();
		indexToId = new HashMap<>();
		links = new HashMap<>();
		try {
			parser = new SMTExpressionParser(new SMTManager());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	TransitionStateMachine(List<DeviceTypeNode> _graph) {
		this();
		graph = _graph;

		int index = 0;
		for (DeviceTypeNode node : graph) {

			for (AttributeData attr : node.getAttributes()) {
				String attrId = String.format("%s:%s", node.getId(), attr.getName());
				indexToId.put(index, attrId);
				links.put(index, attr.getTransitions());
				idToIndex.put(attrId, index++);
			}
		}
		reachable = new ArrayList<Integer>(Collections.nCopies(links.size(), 0));
		solvable = new ArrayList<Integer>(Collections.nCopies(links.size(), 0));
	}

// TODO Implement formula-based BFS search
//	public List<String> proceedSearch(int startId) {
//		ArrayDeque<Integer> q = new ArrayDeque<>();
//		q.addLast(startId);
//		
//		while (!q.isEmpty()) {
//			int current = q.peek();
//			q.pop();
//			used.set(current, 1);
//			
//			if (links.get(current) != null) {
//				for (TransitionData to : links.get(current)) {	
//					String attrId = String.format("%s:%s", to.getDevType(), to.getAttribute());
//					if (used.get(idToIndex.get(attrId)) == 0) {
//						q.addLast(idToIndex.get(attrId));
//					}
//				}
//			}
//		}
//		
//		List<String> availableIds = new ArrayList<String>();
//		
//		for (int i = 0; i < links.size(); i++) {
//			if (used.get(i) == 1) {
//				availableIds.add(indexToId.get(i));
//			}
//		}
//		
//		return availableIds;
//	}

	private Boolean dfs(Integer v) {
		reachable.set(v, 1);

		for (TransitionData to : links.get(v)) {
			String attrId = String.format("%s:%s", to.getDevType(), to.getAttribute());
			// If not visited, go and check cycle there
			if (reachable.get(idToIndex.get(attrId)) == 0 && dfs(idToIndex.get(attrId))) {
				return true;
			}

			// If visited on current search state, cycle found
			if (reachable.get(idToIndex.get(attrId)) == 1) {
				return true;
			}
		}

		reachable.set(v, 2);
		return false;
	}

	public Boolean cycleSearch(String attrName) {
		return dfs(idToIndex.get(attrName));
	}

	private void isSolvable(Integer v, BooleanFormula constraints) {
		if (constraints != null) {
			try (ProverEnvironment prover = parser.formulaManager.context
					.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
				prover.addConstraint(constraints);
				if (prover.isUnsat()) {
					return;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		solvable.set(v, solvable.get(v) + 1);

		for (TransitionData to : links.get(v)) {
			String attrId = String.format("%s:%s", to.getDevType(), to.getAttribute());
			if (constraints == null) {
				isSolvable(idToIndex.get(attrId), parser.parseSMT(to.getFormula()));
			} else {
				isSolvable(idToIndex.get(attrId),
						parser.formulaManager.bmgr.and(constraints, parser.parseSMT(to.getFormula())));
			}
		}
	}

	public String proceedSat(String attrName) {
		isSolvable(idToIndex.get(attrName), null);

		String result = "Unreachable attrs:\n";
		for (int i = 0; i < reachable.size(); i++) {
			if (reachable.get(i) == 2 && solvable.get(i) == 0) {
				result += indexToId.get(i) + "\n";
			}
		}

		return result;
	}

	SMTExpressionParser parser;
	HashMap<String, Integer> idToIndex;
	HashMap<Integer, String> indexToId;
	HashMap<Integer, List<TransitionData>> links;
	List<DeviceTypeNode> graph;
	ArrayList<Integer> reachable;
	ArrayList<Integer> solvable;
}
