package info.doufm.android.network;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by WJ on 2015/1/28.
 */
public class JsonArrayRequestWithCookie extends JsonArrayRequest{
    private Map<String, String> mHeader = new HashMap<>();

    public JsonArrayRequestWithCookie(String url, Response.Listener<JSONArray> listener, Response.ErrorListener errorListener){
        super(url,listener,errorListener);
    }

    //这是个重写与request的函数，估计在volley发送的时候会吧这个cookie加入到包里面发送
    public Map<String, String> getHeaders() throws AuthFailureError{
        return mHeader;
    }

    public void setCookie(String cookie) throws AuthFailureError{
        mHeader.put("Cookie",cookie);
    }

}
