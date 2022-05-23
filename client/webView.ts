type TransitionViewResponse = {
    isOk: boolean;
    message: string;
    root: types.Graph;
    data: Record<types.DiagramLabel['id'], string>;
};

export const getModel = async (): Promise<void> => {
    const uri = await instance.request('activeTextEditorUri');
    if (isErr(uri)) {
        logger.error('Cannot get active editor uri');
        return;
    }
    const response = await instance.lsp.sendRequest('igen/dbg_feature', uri.unwrap()) as TransitionViewResponse;

    if (!response.isOk) {
        logger.error(response.message);
        return;
    }

    if (isGraph(response.root)) {
        diagram.set(reduceGraph(response.root, {}));
        labelsData.set(response.data);
    }
};
