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
 * ���ʲ��ɹ������������̡����̽��������ݸ�NC
 * 
 * @author tengfei.ye@weaver.cn
 * @version 2019/7/18 22:19
 */
public class Zccg2NCAction implements Action{
	private Log log = LogFactory.getLog(Zccg2NCAction.class.getName());
	String requestid = "";
	String billid = "";
	@Override
	public String execute(RequestInfo requestInfo) {
		try {
            log.info("���롶�ʲ��ɹ������������̡����̽�����ֵ��NC����...");
            requestid = requestInfo.getRequestid();
            billid=requestInfo.getRequestManager().getBillid()+"";
            Map<String, String> zccgMainMap = TableDataUtil.getMainMap(log, requestInfo);// ����
            //List<Map<String,String>> listMap1 = TableDataUtil.getDtListMap(log, requestInfo, 0);// ��ϸ��1
            // ����NC-WebService�ӿ�
            String xml = getZccgXml(zccgMainMap);// �ӿڲ���
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
	 * @param zccgMainMap ���ʲ��ɹ������������̡���������
	 * @return NC�涨�Ľӿ���������
	 * @author tengfei.ye@weaver.cn
	 * @version 2019/7/18 22:19
	 */
	private String getZccgXml(Map<String, String> zccgMainMap) {
		StringBuffer xmlSb = new StringBuffer();
		xmlSb.append("<?xml version='1.0' encoding='UTF-8'?>");
		xmlSb.append("<ufinterface billtype='voucher' msg='OA����NC��һ�����ñ�����'>");
		xmlSb.append("<bill>");
		xmlSb.append("<head>");
		xmlSb.append("<pk_m_oa>").append(requestid).append("</pk_m_oa>");// OA���̱��
		xmlSb.append("<pk_org>").append("806").append("</pk_org>");// ��˾��֯
		xmlSb.append("<vouchertype>").append("����ƾ֤").append("</vouchertype>");// ƾ֤��𣺹̶�ֵ ����ƾ֤
		xmlSb.append("<dbilldate>").append(zccgMainMap.get("sqrq")).append("</dbilldate>");// ƾ֤����
		xmlSb.append("<billmaker>").append("hlj").append("</billmaker>");// �Ƶ��˱��� ��ʱֻ��ʹ��hlj
		if (zccgMainMap.get("xgfj") != "") {
			int fjsl = zccgMainMap.get("xgfj").split(",").length;// ��������
			xmlSb.append("<attachment>").append(fjsl).append("</attachment>");// ������
		} else {
			xmlSb.append("<attachment>").append("0").append("</attachment>");// ������
		}
		xmlSb.append("</head>");
		xmlSb.append("<body>");
		double sumjfje = 0.00;// �跽���ϼ�
		//�跽��Ŀ(�ʲ��ɹ���������)��ƾ֤
		xmlSb.append("<detail>");
		xmlSb.append("<detailindex>").append(1).append("</detailindex>");// �к�
		xmlSb.append("<explanation>").append(zccgMainMap.get("zy")).append("</explanation>");// ժҪ
		xmlSb.append("<accsubjcode>").append(zccgMainMap.get("jfkm")).append("</accsubjcode>");// ��Ŀ����
		xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// ����
		xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// �۱�����
		xmlSb.append("<debitamount>").append(zccgMainMap.get("bcfkje")).append("</debitamount>");// �跽���
		xmlSb.append("<creditamount>").append("0.00").append("</creditamount>");// �������
		xmlSb.append("<ass>");
		if(zccgMainMap.get("fphm").equals("")) {//����޷�Ʊ
			if (!zccgMainMap.get("fzhsry").equals("")) {
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("��Ա����").append("</pk_Checktype>");// ��Ա������������
				xmlSb.append("<pk_Checkvalue>").append(zccgMainMap.get("fzhsry")).append("</pk_Checkvalue>");// ��Ա�����������
				xmlSb.append("</item>");
			}
			xmlSb.append("<item>");
			xmlSb.append("<pk_Checktype>").append("�ͻ�����").append("</pk_Checktype>");// �ͻ�����������������
			xmlSb.append("<pk_Checkvalue>").append("10000118").append("</pk_Checkvalue>");// �ͻ����������������
			xmlSb.append("</item>");
		}
		xmlSb.append("</ass>");
		xmlSb.append("</detail>");
		sumjfje=Util.getDoubleValue(zccgMainMap.get("bcfkje"));
		// ������Ŀ��ƾ֤
		xmlSb.append("<detail>");
		xmlSb.append("<detailindex>").append(2).append("</detailindex>");// �к�
		xmlSb.append("<explanation>").append(zccgMainMap.get("zy")).append("</explanation>");// ժҪ
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
