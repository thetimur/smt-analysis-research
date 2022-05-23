/**
 * BdmTransitionGenerator class documentation.
 *
 * This class is a BdmTransitionGenerator representation.
 * It provides some methods needed to build transition diagram from Xtext sources 
 * and prepares it to diagram generation 
 *
 * @author Garaev Timur garaevtimur21@gmail.com.
 *
 * BdmPackage, DeviceDebugPackage -- an interfaces provided by Xtext which helps us to 
 * interact with behavior and composition model entities
 */
class BdmTransitionGenerator {
	
	ITraceProvider traceProvider
	
	new (ITraceProvider _traceProvider) {
		traceProvider = _traceProvider
	}
	
	def generateTransitionGraphFromBdm(List<BdmPackage> packs, DeviceDebugPackage ddm) {
		// Get all types and generate IDM tree
		val Set<EObject> data = newHashSet()
		val typePacks = new ArrayList()
		
		for (obj : packs) {
			typePacks.addAll(
						collectAllPackagesContent(
						    TypePackage,
						    obj.eResource.resourceSet,
						    scopedPackNames(obj as BdmPackage)
						))
			collectAllPackagesTree(TypePackage, obj.eResource.resourceSet, typePacks, data)
		}

        val devTypesTree = data.filter(DeviceType)	
        
        var treeList = new DeviceTypeTreeViewGenerator(traceProvider).generate(
                		 	DevTypeTreeBuilder.buildComponentsByDevTypes(devTypesTree)
                		 )  
                		 
       	var eventList = new ArrayList()
       	for (obj : packs) {
       		eventList.addAll((obj as BdmPackage).getEvents())
       	}       
                		 
        if (ddm === null) {
        	return addTransitions(treeList, eventList, null)
        }
        
        // Generate type transitions except bad ones in debug model 
        return addTransitions(treeList, eventList, new EnvironmentProcessor(ddm).buildDebugTree()) 
	}
	
	/**
	 * addTransitions fills inheritance tree with additional transitions based 
	 * on used-defined events and device setup
	 *
     * @param treeList list of nodes in inheritance tree
	 * @param eventList list of used-definded events
	 * @param root root of current device's tree
	 * @return graph containing used-defined transitions
     */
	def addTransitions(List<DeviceTypeNode> treeList, List<Event> eventList, IDebugDeviceNode root) {
		for (node_index : 0..< treeList.size) {
			for (attr_index :0..< treeList.get(node_index).attributes.size) {
				val linkedEvents = eventList.filter [ event | 
					event.getAttrRef().attr.name == treeList.get(node_index).getAttrName(attr_index)
						&& event.getAttrRef().devT.name == treeList.get(node_index).id
				]
				
				linkedEvents.forEach [ event |
					event.actions.forEach[ action | 
						val targetAttr = switch options : action.getAction() {
							case options instanceof SpecifyData : 
								(options as SpecifyData).getTgtAttr
							case options instanceof CopyData : 
								(options as CopyData).getTgtAttr
							default : null
						}
						
						if (targetAttr !== null) {
							var toType = targetAttr.devT.name
							val toAttr = targetAttr.attr.name
							
							if (!toType.isEmpty && !toAttr.isEmpty 
								&& (root === null || 
									(isTransitionAvailable(toType, targetAttr, root.findChildrenByParentAndAttr(toType, targetAttr)) 
										&& checkEvent(event.when.condition, root)
									)
								)
							) {
								toType = updateType(treeList, toType, toAttr)
								treeList.get(node_index).setAttrTransition(attr_index, toType, toAttr, event.when.condition)	
							}	
						}
					]
				]
			}
		}
		return treeList;
	}
	
	/**
	 * isTransitionAvailable checks if given transition available
	 * on used-defined events and device setup
	 *
     * @param targetType type we want to search
	 * @param targetAttr attribute we want to search
	 * @param nodes composition setup subtree
	 * @return true if transition available
     */
	def isTransitionAvailable(String targetType, DevAttrW targetAttr, List<IDebugDeviceNode> nodes) {
		for (node : nodes) {
			if (node.findAllNodesByAttrScope(
				targetType,
				targetAttr
			).length > 0) {
				return true
			}
		}
		
		return false
	}
	
	/**
	 * checkEvent checks if given event available in given device setup
	 *
     * @param condition event condition data
	 * @param root device setup
	 * @return true if evant is correct available
     */
	def checkEvent(Expression condition, IDebugDeviceNode root) {
		return new EventExpressionChecker().parse(condition, root);
	}
	
	def updateType(List<DeviceTypeNode> treeList, String toType, String toAttr) {
		var result = toType
		while (!hasAttribute(treeList, result, toAttr)) {
			result = getParent(treeList, result);
		}
		return result
	}
	
	def getParent(List<DeviceTypeNode> treeList, String type) {
		treeList.filter[ node  |
			node.getId() == type
		].head.getParent()
	}
	
	def hasAttribute(List<DeviceTypeNode> treeList, String type, String attr) {
		treeList.filter[ node  |
			node.getId() == type
		].head.attributes.filter[ attribute |
			attribute.name == attr
		].size > 0
	}
}