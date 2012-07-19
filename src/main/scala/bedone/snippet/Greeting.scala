package org.bedone.snippet

import java.util.Date
import scala.xml.NodeSeq

class Greeting
{
    /**
     *  橋接動態內容與 HTML 5 模版
     *
     *  @param  html    原本的 HTML
     *  @return         處理過後，要吐給使用者的 HTML
     */
    def render(html: NodeSeq) = {
        
        val currentTime = new Date()

        <div>
            <div>Current Time: {currentTime}</div>
            {html}
            <div>Append something</div>
        </div>
    }
}
