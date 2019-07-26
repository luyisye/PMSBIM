package weaver.pmsbim.eric.action;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.weaver.general.Util;

import weaver.conn.RecordSet;
import weaver.interfaces.workflow.action.Action;
import weaver.pmsbim.eric.util.TableDataUtil;
import weaver.pmsbim.eric.util.XmlUtil;
import weaver.soa.workflow.request.RequestInfo;
/**
 * 《经营费费用报销》流程结束后传数据给NC
 * 
 * @author tengfei.ye@weaver.cn
 * @version 2019/7/18 17:00
 */
public class Jyfybx2NCAction implements Action{
	private Log log = LogFactory.getLog(Jyfybx2NCAction.class.getName());
	String requestid = "";
	String billid = "";
	@Override
	public String execute(RequestInfo requestInfo) {
		try {
            log.info("进入《经营费用报销》流程结束传值给NC方法...");
            requestid = requestInfo.getRequestid();
            billid=requestInfo.getRequestManager().getBillid()+"";
            Map<String, String> clfMainMap = TableDataUtil.getMainMap(log, requestInfo);// 主表
            //List<Map<String,String>> listMap1 = TableDataUtil.getDtListMap(log, requestInfo, 0);// 明细表1
            // 调用NC-WebService接口
            String xml = getJyfyXml(clfMainMap);// 接口参数
            log.info("xml:" + xml);
            weaver.pmsbim.eric.ncWebServiceClient.PMWebserviceLocator locator = new weaver.pmsbim.eric.ncWebServiceClient.PMWebserviceLocator();
            weaver.pmsbim.eric.ncWebServiceClient.PMWebservicePortType service = locator.getPMWebserviceSOAP11port_http();
            String result = service.voucherOrder(xml, "", "", "", "", "", "", "", "", "", "");
            String key = " successful=\"";// NC接口返回值的key：[ successful="]
            int index = result.indexOf(key);// key在返回值字符串中出现的第一次的下标
            String successRst = result.substring(index + key.length(), index + key.length() + 1);// 返回的successful属性值：Y-成功；N-失败
            Map<String, String> contentMap = XmlUtil.xmlToMap(result, "UTF-8");
            if (successRst.equals("Y")) {
                log.info("接口调用成功！返回值为：\n" + result);
            } else {
                log.info("接口调用失败！返回值为：\n" + result);
                requestInfo.getRequestManager().setMessagecontent("接口调用失败！返回值为：\n" + result);
                return "0";
            }
        } catch (Exception e) {
            log.error(e);
        }

        return SUCCESS;
    }

	/**
	 * 获取费用报销XML
	 * 
	 * @param jyfyMainMap 《经营费费用报销》主表数据
	 * @return NC规定的接口所需数据
	 * @author tengfei.ye@weaver.cn
	 * @version 2019/7/18 17:00
	 */
	private String getJyfyXml(Map<String, String> jyfyMainMap) {
		StringBuffer xmlSb = new StringBuffer();
		xmlSb.append("<?xml version='1.0' encoding='UTF-8'?>");
		xmlSb.append("<ufinterface billtype='voucher' msg='OA传送NC的一个费用报销单'>");
		xmlSb.append("<bill>");
		xmlSb.append("<head>");
		xmlSb.append("<pk_m_oa>").append(requestid).append("</pk_m_oa>");// OA流程编号
		xmlSb.append("<pk_org>").append("806").append("</pk_org>");// 公司组织
		xmlSb.append("<vouchertype>").append("记账凭证").append("</vouchertype>");// 凭证类别：固定值 记账凭证
		xmlSb.append("<dbilldate>").append(jyfyMainMap.get("sqrq")).append("</dbilldate>");// 凭证日期
		xmlSb.append("<billmaker>").append("hlj").append("</billmaker>");// 制单人编码 暂时只能使用hlj
		if (jyfyMainMap.get("xgfj") != "") {
			int fjsl = jyfyMainMap.get("xgfj").split(",").length;// 附件数量
			xmlSb.append("<attachment>").append(fjsl).append("</attachment>");// 附件数
		} else {
			xmlSb.append("<attachment>").append("0").append("</attachment>");// 附件数
		}
		xmlSb.append("</head>");
		xmlSb.append("<body>");
		double sumjfje = 0.00;// 借方金额合计
		int inid = 1;
		String sql = "select *,ifnull(sum(rzje),0) as rzjehj from formtable_main_35_dt1 where mainid="+billid+" group by jfkm,fzhsbm,fycdbm,zy";//根据摘要，借方科目，辅助核算项目，费用承担部门分组
		String sqlmain = "select * from formtable_main_35 where requestid="+requestid+"";
		RecordSet rs = new RecordSet();
		RecordSet rs2 = new RecordSet();
		rs.execute(sql);
		rs2.execute(sqlmain);
		while(rs.next()) {
			//sumjfje+=Util.getDoubleValue(rs.getString("rzje"),0.00);
			xmlSb.append("<detail>");
			xmlSb.append("<detailindex>").append(inid).append("</detailindex>");// 行号
			xmlSb.append("<explanation>").append(rs.getString("zy")).append("</explanation>");// 摘要
			xmlSb.append("<accsubjcode>").append(rs.getString("jfkm")).append("</accsubjcode>");// 科目编码
			xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// 币种
			xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// 折本汇率
			xmlSb.append("<debitamount>").append(rs.getString("rzjehj")).append("</debitamount>");// 借方金额
			xmlSb.append("<creditamount>").append("0.00").append("</creditamount>");// 贷方金额
			xmlSb.append("<ass>");
			if (!rs.getString("fzhsbm").equals("")) {
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("部门").append("</pk_Checktype>");// 部门辅助核算名称
				xmlSb.append("<pk_Checkvalue>").append(rs.getString("fzhsbm")).append("</pk_Checkvalue>");// 部门辅助核算编码
				xmlSb.append("</item>");
			}
			if (!rs.getString("yfxm").equals("")) {
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("研发项目").append("</pk_Checktype>");// 项目辅助核算名称
				xmlSb.append("<pk_Checkvalue>").append(getXMBM(rs.getString("yfxm"))).append("</pk_Checkvalue>");// 项目辅助核算编码
				xmlSb.append("</item>");
			}
			xmlSb.append("</ass>");
			xmlSb.append("</detail>");
			inid++;
		}
		//进项税22211601
		String rzcy = jyfyMainMap.get("rzcyhj");//入账差异
		log.info("rzcy:"+rzcy);
		String bxdj = jyfyMainMap.get("bxdj");//报销调碱
		double je = Util.getDoubleValue(rzcy,0.00)-Util.getDoubleValue(bxdj,0.00);
		if(je!=0) {//进项税不为0才能传
			xmlSb.append("<detail>");
			xmlSb.append("<detailindex>").append(inid).append("</detailindex>");// 行号
			xmlSb.append("<explanation>").append(rs.getString("zy")).append("</explanation>");// 摘要
			xmlSb.append("<accsubjcode>").append("22211601").append("</accsubjcode>");// 科目编码
			xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// 币种
			xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// 折本汇率
			xmlSb.append("<debitamount>").append(je).append("</debitamount>");// 借方金额
			xmlSb.append("<creditamount>").append("0.00").append("</creditamount>");// 贷方金额
			xmlSb.append("<ass>");
			xmlSb.append("</ass>");
			xmlSb.append("</detail>");
		}
		// 贷方科目传凭证
		boolean flag=false;
		if(jyfyMainMap.get("hjje")!="") {//如果打款金额不为空
			xmlSb.append("<detail>");
			xmlSb.append("<detailindex>").append(inid+1).append("</detailindex>");// 行号
			xmlSb.append("<explanation>").append(rs.getString("zy")).append("</explanation>");// 摘要
			xmlSb.append("<accsubjcode>").append("1002").append("</accsubjcode>");// 科目编码
			xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// 币种
			xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// 折本汇率
			xmlSb.append("<debitamount>").append("0.00").append("</debitamount>");// 借方金额
			xmlSb.append("<creditamount>").append(Util.getDoubleValue(jyfyMainMap.get("hjje"))).append("</creditamount>");// 贷方金额
			xmlSb.append("<ass>");
			xmlSb.append("<item>");
			xmlSb.append("<pk_Checktype>").append("银行账户").append("</pk_Checktype>");// 辅助核算名称
			xmlSb.append("<pk_Checkvalue>").append("33001616781059888999").append("</pk_Checkvalue>");// 辅助核算编码
			xmlSb.append("</item>");
			xmlSb.append("</ass>");
			xmlSb.append("</detail>");
			flag=true;
		}
		if(jyfyMainMap.get("cdbyj")!="") {//如果冲抵金额不为空
			xmlSb.append("<detail>");
			if(flag) {
				xmlSb.append("<detailindex>").append(inid+2).append("</detailindex>");// 行号
			}else {
				xmlSb.append("<detailindex>").append(inid+1).append("</detailindex>");// 行号
			}		
			xmlSb.append("<explanation>").append(rs.getString("zy")).append("</explanation>");// 摘要
			xmlSb.append("<accsubjcode>").append("122102").append("</accsubjcode>");// 科目编码
			xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// 币种
			xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// 折本汇率
			xmlSb.append("<debitamount>").append("0.00").append("</debitamount>");// 借方金额
			xmlSb.append("<creditamount>").append(Util.getDoubleValue(jyfyMainMap.get("cdbyj"))).append("</creditamount>");// 贷方金额
			xmlSb.append("<ass>");
			xmlSb.append("<item>");
			xmlSb.append("<pk_Checktype>").append("人员档案").append("</pk_Checktype>");// 人员档案辅助核算名称
			xmlSb.append("<pk_Checkvalue>").append(rs2.getString("fzhsry")).append("</pk_Checkvalue>");// 人员档案辅助核算编码
			xmlSb.append("</item>");
			xmlSb.append("<item>");
			xmlSb.append("<pk_Checktype>").append("部门").append("</pk_Checktype>");// 部门辅助核算名称
			xmlSb.append("<pk_Checkvalue>").append(rs.getString("fzhsbm")).append("</pk_Checkvalue>");// 部门辅助核算编码
			xmlSb.append("</item>");
			xmlSb.append("</ass>");
			xmlSb.append("</detail>");
		}
		//---------------------------------------------------------
		xmlSb.append("</body>");
		xmlSb.append("</bill>");
		xmlSb.append("</ufinterface>");
		return xmlSb.toString();
	}

	/* 获取研发项目编码 */
	public String getXMBM(String id) {
		String bm="";
		RecordSet rs = new RecordSet();
		String sql="select * from uf_yfxm where id="+id;
		rs.execute(sql);
		if(rs.next()) {
			bm=rs.getString("yfxmbm");
		}
		return bm;
	}
}
