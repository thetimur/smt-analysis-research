
let lastActiveTextEditorUri: null | vscode.Uri = null;

// Registers web view controller
registerWebviewController(transitionViewInterface, ILanguageClient, {
    panel: true,
    prepare({subscriptions, reveal}) {
        // Register commands
        registerCommand(cmdOpenTransitionView, () => {
            const {activeTextEditor} = vscode.window;
            if (!activeTextEditor) { return }
            lastActiveTextEditorUri = activeTextEditor.document.uri;
            reveal();
        }, subscriptions);
        registerCommand(cmdCheckTransitionCorrectness, async () => {
            const activeEditor = vscode.window.activeTextEditor;
            if (!activeEditor) {
                void showErrorMessage('No opened editor for run csvGenerate command');
                return;
            }

            const languageClient = await inject(ILanguageClient);

            // get current attribute name
            const {line} = activeEditor.selection.active;
            const {character} = activeEditor.selection.active;
            const text = activeEditor.document.getText(new vscode.Range(
                line,
                0,
                line,
                activeEditor.document.lineAt(line).range.end.character
            ));

            const start = text.substring(0, character).lastIndexOf(' ') + 1;
            const end = text.lastIndexOf(' ');
            const attrName = text.substring(start, end);

            const msg = await languageClient.sendRequest<string>('tgen/check_transitions', {
                uri:   activeEditor.document.uri.toString(),
                start: attrName,
            });
            void showInformationMessage(msg);
        }, subscriptions);
        
        registerCommand(cmdCheckEventCondition, async () => {
            const activeEditor = vscode.window.activeTextEditor;
            if (!activeEditor) {
                void showErrorMessage('No opened editor for run csvGenerate command');
                return;
            }

            const languageClient = await inject(ILanguageClient);
            const msg = await languageClient.sendRequest<string>('tgen/check_event_condition', {
                uri:   activeEditor.document.uri.toString(),
            });
            void showInformationMessage(msg, {modal: true});
        }, subscriptions);
        subscriptions.push(asDisposable(() => {
            lastActiveTextEditorUri = null;
        }));
    },
    get title() {
        if (!lastActiveTextEditorUri) {
            return 'Elements';
        }
        return `Elements for ${path.basename(lastActiveTextEditorUri.fsPath)}`;
    },
    requests: {
        activeTextEditorUri() {
            if (!lastActiveTextEditorUri) {
                return Err('Has not activeTextEditor');
            }
            return Ok(lastActiveTextEditorUri
                .toString()
                .replace('%3A', ':'));
        },
    },
    notifications: {},
});
