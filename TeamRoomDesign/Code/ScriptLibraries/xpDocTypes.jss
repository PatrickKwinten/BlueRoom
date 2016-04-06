
function isDiscussion(type) {
	if(null != type) {
		var missionView = database.getView("MissionLookup");
		var missionNote = missionView.getDocumentByKey("Mission");
		var default1;
		if(null != missionNote){
			default1 = missionNote.getItemValue("DefaultDocType1").elementAt(0);
		}
		if(type == strings.defaultdoctype1 || type == default1) {
			return true;
		}else{
			return false;
		}
	}
}

function isActionItem(type) {
	if(null != type) {
		var missionView = database.getView("MissionLookup");
		var missionNote = missionView.getDocumentByKey("Mission");
		var default2;
		if(null != missionNote){
			default2 = missionNote.getItemValue("DefaultDocType2").elementAt(0);
		}
		if(type == strings.defaultdoctype2 || type == default2) {
			return true;
		}else{
			return false;
		}
	}
}

function isMeeting(type) {
	if(null != type) {
		var missionView = database.getView("MissionLookup");
		var missionNote = missionView.getDocumentByKey("Mission");
		var default3;
		if(null != missionNote){
			default3 = missionNote.getItemValue("DefaultDocType3").elementAt(0);
		}
		if(type == strings.defaultdoctype3 || type == default3) {
			return true;
		}else{
			return false;
		}
	}
}

function isReference(type) {
	if(null != type) {
		var missionView = database.getView("MissionLookup");
		var missionNote = missionView.getDocumentByKey("Mission");
		var default4;
		if(null != missionNote){
			default4 = missionNote.getItemValue("DefaultDocType4").elementAt(0);
		}
		if(type == strings.defaultdoctype4 || type == default4) {
			return true;
		}else{
			return false;
		}
	}
}
