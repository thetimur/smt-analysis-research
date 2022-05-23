/**
 * TransitionModelCommands class documentation.
 *
 * This class is a TransitionModelCommands representation.
 * It provides us with a methods that can be called from the client-side of DevM (only in .bdm files)
 *
 * @author Garaev Timur
 */

@JsonSegment('tgen')
class TransitionModelCommands implements ILanguageServerExtension {
	protected extension ILanguageServerAccess access
	Map<String, String> states = newHashMap();
	List<DeviceTypeNode> treeList = newArrayList()
	@Inject ITraceProvider traceProvider

	override void initialize(ILanguageServerAccess access) {
		this.access = access;
	}
    
	@JsonRequest
	def CompletableFuture<String> check_transitions(Map<String, String> data) {
		return this.access.doRead(data.get("uri")) [ context |
			val obj = context?.resource?.contents?.head
			if (obj instanceof BdmPackage) {
				val generator = new BdmTransitionGenerator(traceProvider)

				val treeList = generator.generateTransitionGraphFromBdm(List.of(obj), null)
				val stateMachine = new TransitionStateMachine(treeList)
				val start = data.get("start")
				val attribute = start.substring(start.indexOf('.') + 1, start.length)
				val searchStart = generator.updateType(
					treeList,
					start.substring(0, start.indexOf('.')),
					attribute
				) + ":" + attribute

				if (stateMachine.cycleSearch(searchStart) == false) {
					return stateMachine.proceedSat(searchStart);
				}

				return "Cycle found, bad model"
			} else {
				return "Not a BDM package"
			}
		]
	}

	@JsonRequest
	def CompletableFuture<SModelResponse> dbg_feature(String uri) {
		return this.access.doRead(uri) [ context |
			val pack = context?.resource?.contents?.head
			if (pack instanceof BdmPackage) {
				val treeList = new BdmTransitionGenerator(traceProvider).
					generateTransitionGraphFromBdm(List.of(pack), null)
				val generator = new TransitionDiagramGenerator()
				return generator.generate(
					pack.name,
					treeList,
					"transitionDiaram",
					generator.parseLayoutDefaults(emptyMap)
				)
			} else {
				return new SModelResponse(
					false,
					"There no any BDM packages in this resource set",
					new SModelRoot(),
					new HashMap<String, String>()
				)
			}
		]
	}

	@JsonRequest
	def CompletableFuture<String> check_event_condition(Map<String, String> data) {
		return this.access.doRead(data.get("uri")) [ context |
			val pack = context?.resource?.contents?.head
			if (pack instanceof BdmPackage) {
				var res = new ArrayList<String>();
				for (event : pack.events) {
					val parser = new SMTExpressionParser(new SMTManager());
					val constraint = parser.parseSMT(event.when.condition)

					try (val prover = parser.formulaManager.context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
						prover.addConstraint(constraint);
						if (prover.isUnsat()) {
							res.add(event.name)
						}
					} catch (Exception e) {
						e.message
					}
				}
				return "Unsatisfiable event names:\n" + String.join(",\n", res)
			} else {
				return "There no any BDM packages in this resource set"
			}
		]
	}
}