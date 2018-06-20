<%@taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<table>
  <tr>
    <td class="subBoxTextHdr">Name</td>
    <td class="subBoxText"><c:out value="${targetInstance.profile.name}"/></td>
  </tr>
  <tr>
    <td class="subBoxTextHdr">Description</td>
    <td class="subBoxText"><c:out value="${targetInstance.profile.description}"/></td>
  </tr>
  <tr>
    <td class="subBoxTextHdr">Agency</td>
    <td class="subBoxText"><c:out value="${targetInstance.profile.owningAgency.name}"/></td>
  </tr>      
  <tr>
    <td class="subBoxTextHdr">State</td>
    <td class="subBoxText"><spring:message code="profile.state_${targetInstance.profile.status}"/></td>
  </tr>      
  <tr>
    <td class="subBoxTextHdr">Level</td>
    <td class="subBoxText"><c:out value="${targetInstance.profile.requiredLevel}"/></td>
  </tr>
</table>    


<div style="height: 400px; width: 710px; overflow: scroll; border: 1px solid black; padding: 10px;">
  <pre><c:out value="${targetInstance.profile.profile}"/></pre>
</div>