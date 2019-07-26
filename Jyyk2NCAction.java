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
 * ����Ӫ�ÿ����뵥�����̽��������ݸ�NC
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
            log.info("���롶��Ӫ�ÿ����뵥�����̽�����ֵ��NC����...");
            requestid = requestInfo.getRequestid();
            billid=requestInfo.getRequestManager().getBillid()+"";
            Map<String, String> jyykMainMap = TableDataUtil.getMainMap(log, requestInfo);// ����
            //List<Map<String,String>> listMap1 = TableDataUtil.getDtListMap(log, requestInfo, 0);// ��ϸ��1
            // ����NC-WebService�ӿ�
            String xml = getJyykXml(jyykMainMap);// �ӿڲ���
            log.info("xml:" + xml);
            weaver.pmsbim.eric.ncWebServiceClient.PMWebserviceLocator locator = new weaver.pmsbim.eric.ncWebServiceClient.PMWebserviceLocator();
            weaver.pmsbim.eric.ncWebServiceClient.PMWebservicePortType service = locator.getPMWebserviceSOAP11port_http();
            String result = service.voucherOrder(xml, "", "", "", "", "", "", "", "", "", "");
            String key = " successful=\"";// NC�ӿڷ���ֵ��key��[ successful="]
            int index = result.indexOf(key);// key�ڷ���ֵ�ַ����г��ֵĵ�һ�ε��±�
            String successRst = result.substring(index + key.length(), index + key.length() + 1);// ���ص�successful����ֵ��Y-�ɹ���N-ʧ��
            Map<String, String> contentMap = XmlUtil.xmlToMap(result, "UTF-8");
            if (successRst.equals("Y")) {
                log.info("�ӿڵ��óɹ�������ֵΪ��\n" + result);
            } else {
                log.info("�ӿڵ���ʧ�ܣ�����ֵΪ��\n" + result);
                requestInfo.getRequestManager().setMessagecontent("�ӿڵ���ʧ�ܣ�����ֵΪ��\n" + result);
                return "0";
            }
        } catch (Exception e) {
            log.error(e);
        }

        return SUCCESS;
	}
	/**
	 * ��ȡ���ñ���XML
	 * 
	 * @param jyykMainMap ����Ӫ�ÿ����뵥����������
	 * @return NC�涨�Ľӿ���������
	 * @author tengfei.ye@weaver.cn
	 * @version 2019/7/18 21:38
	 */
	private String getJyykXml(Map<String, String> jyykMainMap) {
		StringBuffer xmlSb = new StringBuffer();
		xmlSb.append("<?xml version='1.0' encoding='UTF-8'?>");
		xmlSb.append("<ufinterface billtype='voucher' msg='OA����NC��һ�����ñ�����'>");
		xmlSb.append("<bill>");
		xmlSb.append("<head>");
		xmlSb.append("<pk_m_oa>").append(requestid).append("</pk_m_oa>");// OA���̱��
		xmlSb.append("<pk_org>").append("806").append("</pk_org>");// ��˾��֯
		xmlSb.append("<vouchertype>").append("����ƾ֤").append("</vouchertype>");// ƾ֤��𣺹̶�ֵ ����ƾ֤
		xmlSb.append("<dbilldate>").append(jyykMainMap.get("sqrq")).append("</dbilldate>");// ƾ֤����
		xmlSb.append("<billmaker>").append("hlj").append("</billmaker>");// �Ƶ��˱��� ��ʱֻ��ʹ��hlj
		if (jyykMainMap.get("xgfj") != "") {
			int fjsl = jyykMainMap.get("xgfj").split(",").length;// ��������
			xmlSb.append("<attachment>").append(fjsl).append("</attachment>");// ������
		} else {
			xmlSb.append("<attachment>").append("0").append("</attachment>");// ������
		}
		xmlSb.append("</head>");
		xmlSb.append("<body>");
		double sumjfje = 0.00;// �跽���ϼ�
		//�跽��Ŀ(��Ӫ�ÿ�)��ƾ֤
		xmlSb.append("<detail>");
		xmlSb.append("<detailindex>").append(1).append("</detailindex>");// �к�
		xmlSb.append("<explanation>").append(jyykMainMap.get("zy")).append("</explanation>");// ժҪ
		xmlSb.append("<accsubjcode>").append(jyykMainMap.get("jfkm")).append("</accsubjcode>");// ��Ŀ����
		xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// ����
		xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// �۱�����
		xmlSb.append("<debitamount>").append(jyykMainMap.get("bcfkje")).append("</debitamount>");// �跽���
		xmlSb.append("<creditamount>").append("0.00").append("</creditamount>");// �������
		xmlSb.append("<ass>");
		if(jyykMainMap.get("yklb").equals("0")) {//�ÿ�������ڶԹ�֧��
			if(jyykMainMap.get("jfkm").equals("122105")) {//
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("��Ա����").append("</pk_Checktype>");// ��Ա������������
				xmlSb.append("<pk_Checkvalue>").append(jyykMainMap.get("fzhsry")).append("</pk_Checkvalue>");// ��Ա�����������
				xmlSb.append("</item>");
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("�ͻ�����").append("</pk_Checktype>");// �ͻ�����������������
				xmlSb.append("<pk_Checkvalue>").append("10000118").append("</pk_Checkvalue>");// �ͻ����������������
				xmlSb.append("</item>");
			}
			if(jyykMainMap.get("jfkm").equals("11230201")){
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("����").append("</pk_Checktype>");// ���Ÿ�����������
				xmlSb.append("<pk_Checkvalue>").append(jyykMainMap.get("fzhsbm")).append("</pk_Checkvalue>");// ���Ÿ����������
				xmlSb.append("</item>");
			}
		}
		if(jyykMainMap.get("yklb").equals("1")) {//�ÿ�������ڱ�֤��Ѻ��
			if (!jyykMainMap.get("fzhsry").equals("")) {
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("��Ա����").append("</pk_Checktype>");// ��Ա������������
				xmlSb.append("<pk_Checkvalue>").append(jyykMainMap.get("fzhsry")).append("</pk_Checkvalue>");// ��Ա�����������
				xmlSb.append("</item>");
			}
			xmlSb.append("<item>");
			xmlSb.append("<pk_Checktype>").append("����").append("</pk_Checktype>");// ���Ÿ�����������
			xmlSb.append("<pk_Checkvalue>").append(jyykMainMap.get("fzhsbm")).append("</pk_Checkvalue>");// ���Ÿ����������
			xmlSb.append("</item>");
			xmlSb.append("<item>");
			xmlSb.append("<pk_Checktype>").append("�ͻ�����").append("</pk_Checktype>");// �ͻ�����������������
			xmlSb.append("<pk_Checkvalue>").append("10000118").append("</pk_Checkvalue>");// �ͻ����������������
			xmlSb.append("</item>");
		}
		
		xmlSb.append("</ass>");
		xmlSb.append("</detail>");
		sumjfje=Util.getDoubleValue(jyykMainMap.get("bcfkje"));
		// ������Ŀ��ƾ֤
		xmlSb.append("<detail>");
		xmlSb.append("<detailindex>").append(2).append("</detailindex>");// �к�
		xmlSb.append("<explanation>").append(jyykMainMap.get("zy")).append("</explanation>");// ժҪ
		xmlSb.append("<accsubjcode>").append("1002").append("</accsubjcode>");// ��Ŀ����
		xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// ����
		xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// �۱�����
		xmlSb.append("<debitamount>").append("0.00").append("</debitamount>");// �跽���
		xmlSb.append("<creditamount>").append(sumjfje).append("</creditamount>");// �������
		xmlSb.append("<ass>");
		xmlSb.append("<item>");
		xmlSb.append("<pk_Checktype>").append("�����˻�").append("</pk_Checktype>");// ������������
		xmlSb.append("<pk_Checkvalue>").append("33001616781059888999").append("</pk_Checkvalue>");// �����������
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
