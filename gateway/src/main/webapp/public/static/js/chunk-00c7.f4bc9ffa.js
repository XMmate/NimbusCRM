(window.webpackJsonp=window.webpackJsonp||[]).push([["chunk-00c7"],{"/9A1":function(e,t,n){"use strict";var r=n("VXrd");n.n(r).a},"/SD7":function(e,t,n){"use strict";var r=n("hxdy");n.n(r).a},"7z6m":function(e,t,n){"use strict";var r=n("iElt");n.n(r).a},"9GgJ":function(e,t,n){"use strict";var r={name:"XrHeader",components:{},props:{iconClass:[String,Array],iconColor:String,label:String,showSearch:{type:Boolean,default:!1},searchIconType:{type:String,default:"text"},placeholder:{type:String,default:"请输入内容"},ftTop:{type:String,default:"15px"},content:[String,Number],inputAttr:{type:Object,default:function(){}}},data:function(){return{search:""}},computed:{},watch:{content:{handler:function(){this.search!=this.content&&(this.search=this.content)},immediate:!0}},mounted:function(){},beforeDestroy:function(){},methods:{inputChange:function(){this.$emit("update:content",this.search)},searchClick:function(){this.$emit("search",this.search)}}},o=(n("zIzm"),n("KHd+")),s=Object(o.a)(r,function(){var e=this,t=e.$createElement,n=e._self._c||t;return n("flexbox",{staticClass:"xr-header"},[e.iconClass?n("div",{staticClass:"xr-header__icon",style:{backgroundColor:e.iconColor}},[n("i",{class:e.iconClass})]):e._e(),e._v(" "),n("div",{staticClass:"xr-header__label"},[e.$slots.label?e._t("label"):[e._v(e._s(e.label))]],2),e._v(" "),e.showSearch?n("el-input",e._b({staticClass:"xr-header__search",class:{"is-text":"text"===e.searchIconType},style:{"margin-top":e.ftTop},attrs:{placeholder:e.placeholder},on:{input:e.inputChange},nativeOn:{keyup:function(t){return"button"in t||!e._k(t.keyCode,"enter",13,t.key,"Enter")?e.searchClick(t):null}},model:{value:e.search,callback:function(t){e.search=t},expression:"search"}},"el-input",e.inputAttr,!1),["text"===e.searchIconType?n("el-button",{attrs:{slot:"append",type:"primary"},nativeOn:{click:function(t){return e.searchClick(t)}},slot:"append"},[e._v("搜索")]):n("el-button",{attrs:{slot:"append",icon:"el-icon-search"},nativeOn:{click:function(t){return e.searchClick(t)}},slot:"append"})],1):e._e(),e._v(" "),n("div",{staticClass:"xr-header__ft",style:{top:e.ftTop}},[e._t("ft")],2)],1)},[],!1,null,"7bba770c",null);s.options.__file="index.vue";t.a=s.exports},Tdi9:function(e,t,n){"use strict";var r=n("jWXv"),o=n.n(r),s=n("rfXi"),a=n.n(s),i=n("jVVe"),l=n("YSp2"),c=n("lNRB"),u=n("jPAu"),d=n("nboU"),p={name:"EditRoleDialog",components:{RoleEmployeeSelect:c.a,WkUserDepSelect:u.a},mixins:[d.a],props:{selectionList:Array,userShow:{type:Boolean,default:!0},visible:{type:Boolean,required:!0,default:!1}},data:function(){return{loading:!1,roleValue:[],ruleForm:{roleList:[],userIds:[],deptIds:[]}}},computed:{title:function(){return this.userShow?"复制角色":"编辑角色"},rules:function(){var e=this,t={roleList:[{required:!0,message:"请选择",trigger:"change"}]};return this.userShow&&(t.userIds=[{validator:function(t,n,r){e.ruleForm.userIds&&e.ruleForm.userIds.length>0||e.ruleForm.deptIds&&e.ruleForm.deptIds.length>0?r():r(new Error("请选择"))},trigger:""}]),t},roleSelectProps:function(){return{roleRequest:l.a}}},watch:{},created:function(){if(this.userShow&&this.selectionList.length>0||!this.userShow&&1===this.selectionList.length){var e=this.selectionList[0];this.ruleForm.roleList=e.roleId?this.selectionList[0].roleId.split(",").map(function(e){return parseFloat(e)}):[]}},methods:{close:function(){this.$emit("update:visible",!1)},sureClick:function(){var e=this;this.$refs.editRoleForm.validate(function(t){if(!t)return!1;var n=[],r=[];e.ruleForm.roleList.forEach(function(e){if("string"==typeof e){if(e.includes("@")){var t=e.split("@");if(t.length>1){var o=t[1].split(",").map(function(e){return parseFloat(e)});r=r.concat(o)}}}else n.push(e)});var s={roleIds:a()(new o.a(n.concat(r)))};e.userShow?(s.userIds=e.ruleForm.userIds,s.deptIds=e.ruleForm.deptIds):s.userIds=e.selectionList.map(function(e){return e.userId}),Object(i.b)(s).then(function(t){e.$message.success("操作成功"),e.$emit("change"),e.close()}).catch(function(){})})}}},h=(n("7z6m"),n("KHd+")),f=Object(h.a)(p,function(){var e=this,t=e.$createElement,n=e._self._c||t;return n("el-dialog",{ref:"wkDialog",attrs:{visible:e.visible,"append-to-body":!0,"close-on-click-modal":!1,width:"500px"},on:{close:e.close}},[n("div",{staticClass:"el-dialog__title",attrs:{slot:"title"},slot:"title"},[e._v("\n    "+e._s(e.title)),e.userShow?n("el-tooltip",{attrs:{effect:"dark",placement:"top"}},[n("div",{attrs:{slot:"content"},slot:"content"},[e._v("1、可以将员工角色复制给其他员工。"),n("br"),e._v("\n        2、若选择的员工已有角色，原角色会被覆盖。"),n("br"),e._v("\n        3、若选择部门，该部门所有员工的角色将相同，"),n("br"),e._v("\n             可保存后再对员工独立调整。\n      ")]),e._v(" "),n("i",{staticClass:"wk wk-help wk-help-tips",staticStyle:{"margin-left":"3px"}})]):e._e()],1),e._v(" "),n("el-form",{ref:"editRoleForm",attrs:{model:e.ruleForm,rules:e.rules,"label-width":"100px","label-position":"top"}},[e.userShow?n("el-form-item",{attrs:{label:"选择员工和部门",prop:"userIds"}},[n("wk-user-dep-select",{staticStyle:{width:"100%"},attrs:{"user-value":e.ruleForm.userIds,"dep-value":e.ruleForm.deptIds},on:{"update:userValue":function(t){e.$set(e.ruleForm,"userIds",t)},"update:depValue":function(t){e.$set(e.ruleForm,"deptIds",t)}}})],1):e._e(),e._v(" "),n("el-form-item",{attrs:{label:"设置角色",prop:"roleList"}},[n("role-employee-select",{staticStyle:{width:"100%"},attrs:{props:e.roleSelectProps,multiple:""},model:{value:e.ruleForm.roleList,callback:function(t){e.$set(e.ruleForm,"roleList",t)},expression:"ruleForm.roleList"}})],1)],1),e._v(" "),n("div",{}),e._v(" "),n("span",{staticClass:"dialog-footer",attrs:{slot:"footer"},slot:"footer"},[n("el-button",{on:{click:e.close}},[e._v("取 消")]),e._v(" "),n("el-button",{attrs:{type:"primary"},on:{click:e.sureClick}},[e._v("确 定")])],1)],1)},[],!1,null,"d6281dcc",null);f.options.__file="EditRoleDialog.vue";t.a=f.exports},VXrd:function(e,t,n){e.exports={xrColorPrimary:"#2362FB"}},YSp2:function(e,t,n){"use strict";n.d(t,"i",function(){return a}),n.d(t,"j",function(){return i}),n.d(t,"k",function(){return l}),n.d(t,"n",function(){return c}),n.d(t,"m",function(){return u}),n.d(t,"l",function(){return d}),n.d(t,"a",function(){return p}),n.d(t,"b",function(){return h}),n.d(t,"c",function(){return f}),n.d(t,"g",function(){return m}),n.d(t,"h",function(){return v}),n.d(t,"f",function(){return b}),n.d(t,"r",function(){return _}),n.d(t,"q",function(){return g}),n.d(t,"p",function(){return y}),n.d(t,"o",function(){return C}),n.d(t,"e",function(){return w}),n.d(t,"d",function(){return x});var r=n("GQeE"),o=n.n(r),s=n("t3Un");function a(e){return Object(s.a)({url:"adminDept/deleteDept/"+e.id,method:"post"})}function i(e){return Object(s.a)({url:"adminDept/setDept",method:"post",data:e,headers:{"Content-Type":"application/json;charset=UTF-8"}})}function l(e){return Object(s.a)({url:"adminDept/addDept",method:"post",data:e,headers:{"Content-Type":"application/json;charset=UTF-8"}})}function c(e){return Object(s.a)({url:"adminUser/setUser",method:"post",data:e,headers:{"Content-Type":"application/json;charset=UTF-8"}})}function u(e){return Object(s.a)({url:"adminUser/addUser",method:"post",data:e,headers:{"Content-Type":"application/json;charset=UTF-8"}})}function d(e){return Object(s.a)({url:"adminRole/getAllRoleList",method:"post",data:e})}function p(e){return Object(s.a)({url:"adminRole/getRoleList",method:"post",data:e})}function h(e){return Object(s.a)({url:"adminRole/queryAuthRole/"+e,method:"post"})}function f(e,t){return Object(s.a)({url:"adminRole/updateAuthRole/"+e,method:"post",data:t,headers:{"Content-Type":"application/json;charset=UTF-8"}})}function m(e){return Object(s.a)({url:"adminUser/resetPassword",method:"post",data:e,headers:{"Content-Type":"application/json;charset=UTF-8"}})}function v(e){return Object(s.a)({url:"adminUser/usernameEdit",method:"post",data:e})}function b(e){return Object(s.a)({url:"adminUser/usernameEditByManager",method:"post",data:e})}function _(e){return Object(s.a)({url:"adminUser/setUserStatus",method:"post",data:e,headers:{"Content-Type":"application/json;charset=UTF-8"}})}function g(e){return Object(s.a)({url:"adminUser/downloadExcel",method:"post",data:e,responseType:"blob"})}function y(e){var t=new FormData;return o()(e).forEach(function(n){t.append(n,e[n])}),Object(s.a)({url:"adminUser/excelImport",method:"post",data:t,headers:{"Content-Type":"multipart/form-data"},timeout:6e4})}function C(e){return Object(s.a)({url:"adminUser/downExcel",method:"post",data:e,responseType:"blob"})}function w(e){return Object(s.a)({url:"adminUser/setUserDept",method:"post",data:e,headers:{"Content-Type":"application/json;charset=UTF-8"}})}function x(){return Object(s.a)({url:"adminUser/countNumOfUser",method:"post"})}},hxdy:function(e,t,n){},iElt:function(e,t,n){},ihDC:function(e,t,n){},jzeO:function(e,t,n){"use strict";var r={name:"Reminder",components:{},props:{closeShow:{type:Boolean,default:!1},content:{type:String,default:"内容"},fontSize:{type:String,default:"13"}},data:function(){return{}},computed:{},mounted:function(){},destroyed:function(){},methods:{close:function(){this.$emit("close")}}},o=(n("/SD7"),n("KHd+")),s=Object(o.a)(r,function(){var e=this,t=e.$createElement,n=e._self._c||t;return n("flexbox",{staticClass:"reminder-wrapper"},[n("flexbox",{staticClass:"reminder-body",attrs:{align:"stretch"}},[n("i",{staticClass:"wk wk-warning reminder-icon"}),e._v(" "),n("div",{staticClass:"reminder-content",style:{"font-size":e.fontSize+"px"},domProps:{innerHTML:e._s(e.content)}}),e._v(" "),e._t("default"),e._v(" "),e.closeShow?n("i",{staticClass:"el-icon-close close",on:{click:e.close}}):e._e()],2)],1)},[],!1,null,"d9a726d6",null);s.options.__file="Reminder.vue";t.a=s.exports},kufG:function(e,t,n){},lNRB:function(e,t,n){"use strict";var r=n("QbLZ"),o=n.n(r),s=n("YSp2"),a=n("KTTK"),i=n("gSIQ"),l=n("oxiq"),c=n.n(l),u=n("jtZb"),d={onlyShowRole:!1,roleRequest:null},p={name:"RoleEmployeeSelect",components:{},props:{props:{type:Object,default:function(){return{}}},value:[Array,Number,String]},data:function(){return{selectValue:[],activeName:"",roleOption:[],userOption:[],searchInput:""}},computed:{config:function(){return Object(u.a)(o()({},d),this.props||{})},select:function(){return this.$refs.select}},watch:{value:{handler:function(){Object(i.valueEquals)(this.value,this.selectValue)||(this.selectValue=this.value)},immediate:!0}},created:function(){this.getRoleList(),this.getUserList()},mounted:function(){},beforeDestroy:function(){},methods:{selectVisibleChange:function(e){""!==this.activeName&&"0"!==this.activeName||(this.activeName="role")},getRoleList:function(){var e=this;(this.config.roleRequest||s.l)().then(function(t){e.roleOption=t.data||[]}).catch(function(){})},getUserList:function(){var e=this;Object(a.u)({pageType:0}).then(function(t){e.userOption=t.data.list||[]}).catch(function(){})},selectChange:function(){this.$emit("input",this.selectValue)},userSearch:function(){var e=this;this.userOption.forEach(function(t){t.isHide=!c.a.match(t.realname,e.searchInput)})}}},h=(n("/9A1"),n("mJFP"),n("KHd+")),f=Object(h.a)(p,function(){var e=this,t=e.$createElement,n=e._self._c||t;return n("el-select",e._g(e._b({ref:"select",staticClass:"role-employee-select",on:{"visible-change":e.selectVisibleChange,change:e.selectChange},model:{value:e.selectValue,callback:function(t){e.selectValue=t},expression:"selectValue"}},"el-select",e.$attrs,!1),e.$listeners),[n("div",{staticClass:"role-employee-select__body"},[n("el-tabs",{ref:"roleTabs",class:{"el-tabs__header--hidden":e.config.onlyShowRole},model:{value:e.activeName,callback:function(t){e.activeName=t},expression:"activeName"}},[n("el-tab-pane",{ref:"roleTabPane",attrs:{label:"自选角色",name:"role"}},e._l(e.roleOption,function(t){return n("div",{key:t.pid,attrs:{label:t.name}},[n("div",{staticClass:"role-employee-select__title"},[e._v(e._s(t.name))]),e._v(" "),e._l(t.list,function(e){return n("el-option",{key:e.roleId,staticStyle:{padding:"0 10px"},attrs:{label:e.roleName,value:e.roleId}})})],2)})),e._v(" "),n("el-tab-pane",{attrs:{label:"按员工复制角色",name:"employee"}},[n("el-input",{staticClass:"search-input",attrs:{placeholder:"搜索成员",size:"small","prefix-icon":"el-icon-search"},on:{input:e.userSearch},model:{value:e.searchInput,callback:function(t){e.searchInput=t},expression:"searchInput"}}),e._v(" "),e._l(e.userOption,function(t){return n("el-option",{directives:[{name:"show",rawName:"v-show",value:!t.isHide,expression:"!item.isHide"}],key:t.userId,staticStyle:{padding:"0 10px"},attrs:{label:t.realname,value:t.userId+"@"+t.roleId}},[n("flexbox",{staticClass:"cell"},[n("xr-avatar",{staticClass:"cell__img",attrs:{name:t.realname,size:24,src:t.img}}),e._v(" "),n("div",{staticClass:"cell__body"},[e._v(e._s(t.realname))]),e._v(" "),n("el-tooltip",{attrs:{content:t.roleName,effect:"dark",placement:"top"}},[n("div",{staticClass:"cell__footer text-one-line"},[e._v(e._s(t.roleName))])])],1)],1)})],2)],1)],1)])},[],!1,null,"22b1a213",null);f.options.__file="RoleEmployeeSelect.vue";t.a=f.exports},mJFP:function(e,t,n){"use strict";var r=n("kufG");n.n(r).a},nboU:function(e,t,n){"use strict";t.a={watch:{loading:function(e){if(e){var t=this.$refs.wkDialog.$refs.dialog;this.loadingInstance=this.$loading({target:t})}else this.loadingInstance&&this.loadingInstance.close()}}}},zIzm:function(e,t,n){"use strict";var r=n("ihDC");n.n(r).a}}]);