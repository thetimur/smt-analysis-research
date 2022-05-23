/**
 * TransitionDiagramGenerator class documentation.
 *
 * This class is a TransitionDiagramGenerator representation.
 * It provides us with a method that sequentially generates a graph, draws it 
 * and sends to client in a convenient format
 *
 * @author Garaev Timur
 */

public class TransitionDiagramGenerator {
	private static final String PORT_LEFT = "-port-left";
	private static final String PORT_RIGHT = "-port-right";

	/* IGraphLayoutEngine -- an interface ELK gives us to
	* choose which layout engine we want to use
	*
	* Here we decided to use RecursiveGraphLayoutEngine because it 
	* works great with nested vertices and hierarchical structures
	*
	* SModelElement -- part of Sprotty (https://github.com/eclipse/sprotty) diagram layout framework.
	* Provides us an interface for description and filling of chart elements during diagram construction
	*
	* ElkNode -- part of Eclipse Layout Kernel (https://www.eclipse.org/elk/) framework needed to compose 
	* and layout graph we gonna send to client side
	*/
	private IGraphLayoutEngine engine = new RecursiveGraphLayoutEngine();
    
    public SModelResponse generate(String packName, List<DeviceTypeNode> typeGraphNodes, String diagramType, LayoutDefault defaults) {
        SGraph graph = new SGraph(it -> {
            it.setType("transition:diagram");
            it.setId(packName + "-diagram");
        });
        
        HashMap<String, String> labels = new HashMap<String, String>();
        HashMap<String, String> label_data = new HashMap<String, String>();
        HashMap<String, Boolean> needAttrIds = new HashMap<>();
        collectNeededAttrs(typeGraphNodes, needAttrIds);
        
        // Generate graph from typeGraphNodes
        
        List<SModelElement> children = new ArrayList<>();
        for (DeviceTypeNode node : typeGraphNodes) {
        	// Add and setup nodes
        	SNodeExt curNode = new SNodeExt(
                    new HashMap<>(),
                    it -> {
                        it.setId(node.getId());
                        it.setType("node:type");
                        it.setChildren(new ArrayList<>());
                    }
                );
        	curNode.getProperties().put(CoreOptions.NODE_LABELS_PLACEMENT, EnumSet.of(
	                NodeLabelPlacement.H_CENTER,
	                NodeLabelPlacement.V_TOP,
	                NodeLabelPlacement.INSIDE   
	            ));
        	curNode.getProperties().put(CoreOptions.NODE_SIZE_CONSTRAINTS, 
        			EnumSet.of(SizeConstraint.MINIMUM_SIZE, SizeConstraint.NODE_LABELS)
        		);
        	
        	// Add attribute subnodes
        	List<SModelElement> subNodes = new ArrayList<>();
        	
        	subNodes.addAll(
    			List.of(
	    			new SLabel((it1) -> {
		                it1.setId(node.getId());
		                it1.setText(node.getId());
		            }),
	        		new SPort(it1 -> {
	                    it1.setId(getTargetPortId(node));
	                    it1.setChildren(new ArrayList<>());
	                }),
	        		new SPort(it1 -> {
	                    it1.setId(getSourcePortId(node));
	                    it1.setChildren(new ArrayList<>());
	                })
	            ));
        	
        	for (AttributeData attr : node.getAttributes()) {
        		if (!needAttrIds.containsKey(getAttrNodeId(node, attr))) {
        			continue;
        		}
        		
        		SNodeExt attrNode = new SNodeExt(
                        new HashMap<>(),
                        it -> {
                            it.setId(getAttrNodeId(node, attr));
                            it.setType("node:attr");
                            it.setChildren(List.of(
                            		new SLabel((it1) -> {
                                        it1.setId(getNodeLabelId(node, attr));
                                        it1.setText(getAttrNodeId(node, attr));
                                    }),
                            		new SPort(it1 -> {
    		                            it1.setId(getSourcePortId(node, attr));
    		                            it1.setChildren(new ArrayList<>());
    		                        }),
                            		new SPort(it1 -> {
    		                            it1.setId(getTargetPortId(node, attr));
    		                            it1.setChildren(new ArrayList<>());
    		                        })
                            	));
                        }
                    );
        		attrNode.getProperties().put(CoreOptions.NODE_LABELS_PLACEMENT, EnumSet.of(
    	                NodeLabelPlacement.H_CENTER,
    	                NodeLabelPlacement.V_CENTER,
    	                NodeLabelPlacement.INSIDE   
    	            ));
            	attrNode.getProperties().put(CoreOptions.NODE_SIZE_CONSTRAINTS, 
            			EnumSet.of(SizeConstraint.MINIMUM_SIZE, SizeConstraint.NODE_LABELS)
                	);

            	subNodes.add(attrNode);
            	
            	
            	// Add transition edges
            	if (attr.getTransitions() != null) {
            		
            		// Setup unique labels for edges between similar nodes
            		for (TransitionData data : attr.getTransitions()) {
            			String source = getSourcePortId(node, attr);
            			String target = getTargetPortId(data);
            			
            			String id = getEdgeId(source, target);
            			
            			if (labels.get(id) != null) {
            				labels.put(id, labels.get(id) + "\n" + data.getFormulaRepresentation());
            			} else {
            				labels.put(id, data.getFormulaRepresentation());
            			}
            		}
            		
            		for (TransitionData data : attr.getTransitions()) {
            			String source = getSourcePortId(node, attr);
            			String target = getTargetPortId(data);
            			
            			String id = getEdgeId(source, target);
            			if (labels.get(id) == null) {
            				continue;
            			}
            			
            			children.add(new SEdge(it -> {
                            it.setSourceId(source);
                            it.setTargetId(target);
                            it.setType("edge:transition");
                            it.setId(id);
                            it.setChildren(List.of(
                            		new SLabel((it1) -> {
                                        it1.setId(getLabelId(source, target));
                                        it1.setText("Conditions:");
                                    }))
                            	);
                        }));
            			label_data.put(getLabelId(source, target), labels.get(id));
            			labels.remove(id);
            		}
            	}
        	}
        	curNode.setChildren(subNodes);
        	children.add(curNode);
        	
        	// Add edges between types
        	if (node.getParent() != null) {
        		children.add(new SEdge(it -> {
                    it.setSourceId(getSourcePortId(node.getParent()));
                    it.setTargetId(getTargetPortId(node));
                    it.setType("edge:type");
                    it.setId(getEdgeId(it.getSourceId(), it.getTargetId()));
                }));
        	}
        }
        
        graph.setChildren(children);
        ElkNode elkGraph = new ElkTransformer(defaults).transformTransitionToElk(graph);
        engine.layout(elkGraph, new BasicProgressMonitor());
        applyBounds(graph, elkGraph);
        return new SModelResponse(true, "Diagram opened", graph, label_data);
    }

	private String getSourcePortId(DeviceTypeNode node) {
		return getNodeId(node) + PORT_RIGHT;
	}
	
	private String getSourcePortId(String node) {
		return node + PORT_RIGHT;
	}

	private String getSourcePortId(DeviceTypeNode node, AttributeData attr) {
		return getAttrNodeId(node, attr) + PORT_RIGHT;
	}

	private String getTargetPortId(DeviceTypeNode node) {
		return getNodeId(node) + PORT_LEFT;
	}
	
	private String getTargetPortId(TransitionData data) {
    	return data.getDevType() + ":" + data.getAttribute() + PORT_LEFT;
	}

	private String getTargetPortId(DeviceTypeNode node, AttributeData attr) {
    	return getAttrNodeId(node, attr) + PORT_LEFT;
	}
	
	private String getAttrNodeId(DeviceTypeNode node, AttributeData attr) {
    	return node.getId() + ":" + attr.getName();
	}
	private String getAttrNodeId(String devType, String attribute) {
		return devType + ":" + attribute;
	}

	private String getNodeLabelId(DeviceTypeNode node, AttributeData attr) {
    	return node.getId() + ":" + attr.getName() + "-label";
	}

	private String getLabelId(String source, String target) {
    	return source + "2" + target + "-label";
    }
    
    private String getEdgeId(String source, String target) {
    	return source + "2" + target + "-edge";
    }
    
	private String getNodeId(DeviceTypeNode node) {
		return node.getId();
	}
    

	/**
	 * collectNeededAttrs collects only attributes which we gonna show in current bdm setup
     * @param typeGraphNodes inheritance model tree
	 * @param needAttrIds map of needed attribute ids we want to fill with data
     */
    private void collectNeededAttrs(List<DeviceTypeNode> typeGraphNodes, HashMap<String, Boolean> needAttrIds) {
    	 for (DeviceTypeNode node : typeGraphNodes) {
    		 for (AttributeData attr : node.getAttributes()) {
    			 if (attr.getTransitions() != null) {
             		for (TransitionData data : attr.getTransitions()) {
             			needAttrIds.put(getAttrNodeId(node, attr), true);
             			needAttrIds.put(getAttrNodeId(data.getDevType(), data.getAttribute()), true);
             		}
    			 }
    		 }
    	 }
	}

	/**
	 * applyBounds determines the required dimensions and positions in Sprotty diagram
	 * for already generated ELK graph 
	 * and expands them if necessary
     * @param sNode Sprotty node we wanna expand
	 * @param elkNode node corresponding to sNode in another ELK diagram
     */
	public static void applyBounds(BoundsAware sNode, ElkNode elkNode) {
        sNode.setPosition(new Point(elkNode.getX(), elkNode.getY()));
        sNode.setSize(new Dimension(elkNode.getWidth(), elkNode.getHeight()));
        
        if (!(sNode instanceof SModelElement)) {
            return;
        }
        List<SModelElement> children = ((SModelElement) sNode).getChildren();
        if (children == null) {
            return;
        }
        int elkChildInd = 0;
        EList<ElkNode> elkChildren = elkNode.getChildren();
        for (SModelElement elem : children) {
            if (elem instanceof SNode) {
                int curInd = elkChildInd;
                do {
                    ElkNode curElkNode = elkChildren.get(curInd);
                    if (elem.getId().equals(curElkNode.getIdentifier())) {
                        applyBounds((BoundsAware) elem, curElkNode);
                        elkChildInd = (curInd + 1) % elkChildren.size();
                        break;
                    }
                    curInd = (curInd + 1) % elkChildren.size();
                } while (curInd != elkChildInd);
            } else if (elem instanceof SLabel) {
                for (ElkLabel elkLabel : elkNode.getLabels()) {
                    if (elkLabel.getIdentifier().equals(elem.getId())) {
                        ((SLabel) elem).setPosition(new Point(elkLabel.getX(), elkLabel.getY()));
                        ((SLabel) elem).setSize(new Dimension(elkLabel.getWidth(), elkLabel.getHeight()));
                    }
                }
            } else if (elem instanceof SEdge) {
            	for (ElkEdge edge : elkNode.getContainedEdges()) {
            		if (edge.getIdentifier().equals(elem.getId())) {
            			List<ElkEdgeSection> sections = edge.getSections();
                		if (edge.getSections() == null || edge.getSections().size() == 0) {
                			continue;
                		}
                		List<Point> path = new ArrayList<>();
                		path.add(new Point(
    	            				sections.get(0).getStartX(),
    	            				sections.get(0).getStartY())
                				);
                		
                		for (ElkEdgeSection cur : sections) {
                			if (cur.getBendPoints() != null) {
                				for (ElkBendPoint bend : cur.getBendPoints()) {
                					path.add(new Point(
            	            				bend.getX(),
            	            				bend.getY())
                        				);
                				}
                			}
                			path.add(new Point(
    	            				cur.getEndX(),
    	            				cur.getEndY())
                				);
                		}
                		
                		for (ElkLabel elkLabel : edge.getLabels()) {
                            for (SModelElement sLabel : ((SEdge)elem).getChildren()) {
                            	if (elkLabel.getIdentifier() == sLabel.getId()) {
                            		 ((SLabel) sLabel).setPosition(new Point(elkLabel.getX(), elkLabel.getY()));
                                     ((SLabel) sLabel).setSize(new Dimension(elkLabel.getWidth(), elkLabel.getHeight()));
                                     break;
                            	}
                            }
                        }
                		
                		((SEdge) elem).setRoutingPoints(path);
                		break;
            		}
            	}
            }
        }
    }
    
	/**
	 * parseLayoutDefaults applies some layout default variables that can be setup 
	 * by client-side user
     * @param rawDefaults default variables map
     */
    @SuppressWarnings("unchecked")
    public LayoutDefault parseLayoutDefaults(Map<String, Object> rawDefaults) {
        Map<String, Defaults> typeToDefs = new HashMap<>();
        for (Map.Entry<String, Object> typeToMap : rawDefaults.entrySet()) {      
			Map<String, Object> nameToVal = (Map<String, Object>) typeToMap.getValue();
            Map<String, Double> szToVal = (Map<String, Double>) nameToVal.get("defaultSize");
            Map<String, Double> fontToVal = (Map<String, Double>) nameToVal.get("fontSize");
            typeToDefs.put(typeToMap.getKey(), new Defaults(
                szToVal.get("width"),
                szToVal.get("height"),
                (int) Math.round(fontToVal.get("size")),
                fontToVal.get("width"),
                fontToVal.get("height")
            ));
        }
        return new LayoutDefault(typeToDefs);
    }
}
