import org.eclipse.sprotty.SModelRoot
import java.util.HashMap

class SModelResponse {
	val Boolean isOk
	val String message
	val SModelRoot root
	val HashMap<String, String> data
	
	new (boolean status, String _message, SModelRoot _root, HashMap<String, String> _data) {
		this.isOk = status
		this.message = _message
		this.root = _root
		this.data = _data
	}
}