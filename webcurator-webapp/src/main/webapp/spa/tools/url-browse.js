var _browse_type='WCTREVIEW';
var _browse_base_url='/curator/tools/content';

function browseUrl(data){
	_browse_type=_browse_type.toUpperCase();
	var url;
	if (_browse_type==='WCTREVIEW') {
		if(data.contentType==='text/html') {		
			// url=_browse_base_url+'/browse/'+harvestResultId+'/?url='+decodeURI(data.url);
			url=_browse_base_url+'/browse/'+harvestResultId+'/?url='+btoa(data.url);
		}else{
			url=_browse_base_url+'/download/'+harvestResultId+'/?url='+btoa(data.url);
		}
	} else if(_browse_type==='LIVESITE'){
		url=data.url;
	}else{
		url=_browse_base_url+'?'+data.url;
	}
	window.open(url);
}

function downloadUrl(data){
	_browse_type=_browse_type.toUpperCase();

	var url;
	if (_browse_type==='WCTREVIEW') {
		url=_browse_base_url+'/download/'+harvestResultId+'/?url='+btoa(data.url);
	}else if(_browse_type==='LIVESITE'){
		url=data.url;
	}else{
		url=_browse_base_url+'?'+data.url;
	}

    var a = document.createElement("a");
    document.body.appendChild(a);
    a.style = "display: none";
    a.href = url;
    a.download = data.url;
    a.click();
}