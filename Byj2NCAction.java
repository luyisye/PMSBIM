package weaver.pmsbim.eric.action;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.weaver.general.Util;

import weaver.interfaces.workflow.action.Action;
import weaver.pmsbim.eric.util.TableDataUtil;
import weaver.pmsbim.eric.util.XmlUtil;
import weaver.soa.workflow.request.RequestInfo;

/**
 * 《备用金用款申请》流程结束后传数据给NC
 * 
 * @author tengfei.ye@weaver.cn
 * @version 2019/7/18 16:00
 */
public class Byj2NCAction implements Action{
	private Log log = LogFactory.getLog(Clfbx2NCAction.class.getName());
	String requestid = "";
	List<Map<String, String>> listMap2 = null;
	@Override
	public String execute(RequestInfo requestInfo) {
		try {
			requestid = requestInfo.getRequestid();
			log.info("进入《备用金用款申请》流程结束传值给NC方法...");
			Map<String, String> byjMainMap = TableDataUtil.getMainMap(log, requestInfo);// 主表
			// 调用NC-WebService接口
			String xml = getClfXml(byjMainMap);// 接口参数
			log.info("xml:" + xml);
			weaver.pmsbim.eric.ncWebServiceClient.PMWebserviceLocator locator = new weaver.pmsbim.eric.ncWebServiceClient.PMWebserviceLocator();
			weaver.pmsbim.eric.ncWebServiceClient.PMWebservicePortType service = locator
					.getPMWebserviceSOAP11port_http();
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
 * @param byjMainMap 《备用金用款申请》主表数据
 * @return NC规定的接口所需数据
 * @author tengfei.ye@weaver.cn
 * @version 2019/7/18 14:00
 */
private String getClfXml(Map<String, String> byjMainMap) {
	StringBuffer xmlSb = new StringBuffer();
	xmlSb.append("<?xml version='1.0' encoding='UTF-8'?>");
	xmlSb.append("<ufinterface billtype='voucher' msg='OA传送NC的一个费用报销单'>");
	xmlSb.append("<bill>");
	xmlSb.append("<head>");
	xmlSb.append("<pk_m_oa>").append(requestid).append("</pk_m_oa>");// OA流程编号
	xmlSb.append("<pk_org>").append("806").append("</pk_org>");// 公司组织
	xmlSb.append("<vouchertype>").append("记账凭证").append("</vouchertype>");// 凭证类别：固定值 记账凭证
	xmlSb.append("<dbilldate>").append(byjMainMap.get("sqrq")).append("</dbilldate>");// 凭证日期
	xmlSb.append("<billmaker>").append("hlj").append("</billmaker>");// 制单人编码 暂时只能使用hlj
	if (byjMainMap.get("xgfj") != "") {
		int fjsl = byjMainMap.get("xgfj").split(",").length;// 附件数量
		xmlSb.append("<attachment>").append(fjsl).append("</attachment>");// 附件数
	} else {
		xmlSb.append("<attachment>").append("0").append("</attachment>");// 附件数
	}
	xmlSb.append("</head>");
	xmlSb.append("<body>");
	double sumjfje = 0.00;// 借方金额合计
	//借方科目(备用金)传凭证
	xmlSb.append("<detail>");
	xmlSb.append("<detailindex>").append(1).append("</detailindex>");// 行号
	xmlSb.append("<explanation>").append(byjMainMap.get("zy")).append("</explanation>");// 摘要
	xmlSb.append("<accsubjcode>").append(byjMainMap.get("jfkm")).append("</accsubjcode>");// 科目编码
	xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// 币种
	xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// 折本汇率
	xmlSb.append("<debitamount>").append(byjMainMap.get("byje")).append("</debitamount>");// 借方金额
	xmlSb.append("<creditamount>").append("0.00").append("</creditamount>");// 贷方金额
	xmlSb.append("<ass>");
	if (!byjMainMap.get("fzhsbm").equals("")) {
		xmlSb.append("<item>");
		xmlSb.append("<pk_Checktype>").append("部门").append("</pk_Checktype>");// 部门辅助核算名称
		xmlSb.append("<pk_Checkvalue>").append(byjMainMap.get("fzhsbm")).append("</pk_Checkvalue>");// 部门辅助核算编码
		xmlSb.append("</item>");
	}
	if (!byjMainMap.get("fzhsry").equals("")) {
		xmlSb.append("<item>");
		xmlSb.append("<pk_Checktype>").append("人员档案").append("</pk_Checktype>");// 人员档案辅助核算名称
		xmlSb.append("<pk_Checkvalue>").append(byjMainMap.get("fzhsry")).append("</pk_Checkvalue>");// 人员档案辅助核算编码
		xmlSb.append("</item>");
	}
	xmlSb.append("</ass>");
	xmlSb.append("</detail>");
	sumjfje=Util.getDoubleValue(byjMainMap.get("byje"));
	// 贷方科目传凭证
	xmlSb.append("<detail>");
	xmlSb.append("<detailindex>").append(2).append("</detailindex>");// 行号
	xmlSb.append("<explanation>").append(byjMainMap.get("zy")).append("</explanation>");// 摘要
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
