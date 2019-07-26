package weaver.pmsbim.eric.action;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.weaver.general.Util;

import weaver.interfaces.workflow.action.Action;
import weaver.pmsbim.eric.util.TableDataUtil;
import weaver.pmsbim.eric.util.XmlUtil;
import weaver.soa.workflow.request.RequestInfo;
/**
 * 《经营用款申请单》流程结束后传数据给NC
 * 
 * @author tengfei.ye@weaver.cn
 * @version 2019/7/18 21:38
 */
public class Jyyk2NCAction implements Action{
	private Log log = LogFactory.getLog(Jyyk2NCAction.class.getName());
	String requestid = "";
	String billid = "";
	public String execute(RequestInfo requestInfo) {
		try {
            log.info("进入《经营用款申请单》流程结束传值给NC方法...");
            requestid = requestInfo.getRequestid();
            billid=requestInfo.getRequestManager().getBillid()+"";
            Map<String, String> jyykMainMap = TableDataUtil.getMainMap(log, requestInfo);// 主表
            //List<Map<String,String>> listMap1 = TableDataUtil.getDtListMap(log, requestInfo, 0);// 明细表1
            // 调用NC-WebService接口
            String xml = getJyykXml(jyykMainMap);// 接口参数
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
	 * @param jyykMainMap 《经营用款申请单》主表数据
	 * @return NC规定的接口所需数据
	 * @author tengfei.ye@weaver.cn
	 * @version 2019/7/18 21:38
	 */
	private String getJyykXml(Map<String, String> jyykMainMap) {
		StringBuffer xmlSb = new StringBuffer();
		xmlSb.append("<?xml version='1.0' encoding='UTF-8'?>");
		xmlSb.append("<ufinterface billtype='voucher' msg='OA传送NC的一个费用报销单'>");
		xmlSb.append("<bill>");
		xmlSb.append("<head>");
		xmlSb.append("<pk_m_oa>").append(requestid).append("</pk_m_oa>");// OA流程编号
		xmlSb.append("<pk_org>").append("806").append("</pk_org>");// 公司组织
		xmlSb.append("<vouchertype>").append("记账凭证").append("</vouchertype>");// 凭证类别：固定值 记账凭证
		xmlSb.append("<dbilldate>").append(jyykMainMap.get("sqrq")).append("</dbilldate>");// 凭证日期
		xmlSb.append("<billmaker>").append("hlj").append("</billmaker>");// 制单人编码 暂时只能使用hlj
		if (jyykMainMap.get("xgfj") != "") {
			int fjsl = jyykMainMap.get("xgfj").split(",").length;// 附件数量
			xmlSb.append("<attachment>").append(fjsl).append("</attachment>");// 附件数
		} else {
			xmlSb.append("<attachment>").append("0").append("</attachment>");// 附件数
		}
		xmlSb.append("</head>");
		xmlSb.append("<body>");
		double sumjfje = 0.00;// 借方金额合计
		//借方科目(经营用款)传凭证
		xmlSb.append("<detail>");
		xmlSb.append("<detailindex>").append(1).append("</detailindex>");// 行号
		xmlSb.append("<explanation>").append(jyykMainMap.get("zy")).append("</explanation>");// 摘要
		xmlSb.append("<accsubjcode>").append(jyykMainMap.get("jfkm")).append("</accsubjcode>");// 科目编码
		xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// 币种
		xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// 折本汇率
		xmlSb.append("<debitamount>").append(jyykMainMap.get("bcfkje")).append("</debitamount>");// 借方金额
		xmlSb.append("<creditamount>").append("0.00").append("</creditamount>");// 贷方金额
		xmlSb.append("<ass>");
		if(jyykMainMap.get("yklb").equals("0")) {//用款类别属于对公支付
			if(jyykMainMap.get("jfkm").equals("122105")) {//
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("人员档案").append("</pk_Checktype>");// 人员辅助核算名称
				xmlSb.append("<pk_Checkvalue>").append(jyykMainMap.get("fzhsry")).append("</pk_Checkvalue>");// 人员辅助核算编码
				xmlSb.append("</item>");
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("客户档案").append("</pk_Checktype>");// 客户档案辅助核算名称
				xmlSb.append("<pk_Checkvalue>").append("10000118").append("</pk_Checkvalue>");// 客户档案辅助核算编码
				xmlSb.append("</item>");
			}
			if(jyykMainMap.get("jfkm").equals("11230201")){
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("部门").append("</pk_Checktype>");// 部门辅助核算名称
				xmlSb.append("<pk_Checkvalue>").append(jyykMainMap.get("fzhsbm")).append("</pk_Checkvalue>");// 部门辅助核算编码
				xmlSb.append("</item>");
			}
		}
		if(jyykMainMap.get("yklb").equals("1")) {//用款类别属于保证金押金
			if (!jyykMainMap.get("fzhsry").equals("")) {
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("人员档案").append("</pk_Checktype>");// 人员辅助核算名称
				xmlSb.append("<pk_Checkvalue>").append(jyykMainMap.get("fzhsry")).append("</pk_Checkvalue>");// 人员辅助核算编码
				xmlSb.append("</item>");
			}
			xmlSb.append("<item>");
			xmlSb.append("<pk_Checktype>").append("部门").append("</pk_Checktype>");// 部门辅助核算名称
			xmlSb.append("<pk_Checkvalue>").append(jyykMainMap.get("fzhsbm")).append("</pk_Checkvalue>");// 部门辅助核算编码
			xmlSb.append("</item>");
			xmlSb.append("<item>");
			xmlSb.append("<pk_Checktype>").append("客户档案").append("</pk_Checktype>");// 客户档案辅助核算名称
			xmlSb.append("<pk_Checkvalue>").append("10000118").append("</pk_Checkvalue>");// 客户档案辅助核算编码
			xmlSb.append("</item>");
		}
		
		xmlSb.append("</ass>");
		xmlSb.append("</detail>");
		sumjfje=Util.getDoubleValue(jyykMainMap.get("bcfkje"));
		// 贷方科目传凭证
		xmlSb.append("<detail>");
		xmlSb.append("<detailindex>").append(2).append("</detailindex>");// 行号
		xmlSb.append("<explanation>").append(jyykMainMap.get("zy")).append("</explanation>");// 摘要
		xmlSb.append("<accsubjcode>").append("1002").append("</accsubjcode>");// 科目编码
		xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// 币种
		xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// 折本汇率
		xmlSb.append("<debitamount>").append("0.00").append("</debitamount>");// 借方金额
		xmlSb.append("<creditamount>").append(sumjfje).append("</creditamount>");// 贷方金额
		xmlSb.append("<ass>");
		xmlSb.append("<item>");
		xmlSb.append("<pk_Checktype>").append("银行账户").append("</pk_Checktype>");// 辅助核算名称
		xmlSb.append("<pk_Checkvalue>").append("33001616781059888999").append("</pk_Checkvalue>");// 辅助核算编码
		xmlSb.append("</item>");
		xmlSb.append("</ass>");
		xmlSb.append("</detail>");
		//---------------------------------------------------------
		xmlSb.append("</body>");
		xmlSb.append("</bill>");
		xmlSb.append("</ufinterface>");
		return xmlSb.toString();
	}
}
