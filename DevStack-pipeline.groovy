/**
 * Create nodes 
 *
 * Expected parameters:
 *   DEVSTACK_REPO
 *   DEVSTACK_BRANCH
 *   LOCAL_CONF
**/
 

// node {
  def LIBVIRT_SOCKET = "--connect=qemu:///system"
//  if (DISTRO_RELEASE == xenial){
  def old_img = 'devstack-generic-ubuntu-xenial'
//    else {
//   'Distro release not present'
//  }
  def new_img = "${env.ENV_NAME}"

  def SSHUSER = "cirros"
  def SSHPASS = "cubswin:)"

     stage ('Create VM') {
       sh "set -xe && printenv"
       sh "virt-clone ${LIBVIRT_SOCKET} -o ${old_img} -n ${new_img} --auto-clone || true"
       sh "virsh start ${new_img} || true"
       def mac = sh(script: "virsh domiflist ${new_img} | awk '/virbr0/ {print \$5}'", returnStdout: true)
       println mac.text
       def IP = sh(script: "/usr/sbin/arp -an  |grep ${mac} |awk '{print \$2}'", returnStdout: true)
       IP = IP.replaceAll('\\)','')
       def ENV_IP = IP.replaceAll('\\(','')
      }   
    
      stage ('Deploy Devstack') {
        writeFile file: WORKSPACE + '/ssh-config', text: """\
          StrictHostKeyChecking no
          UserKnownHostsFile /dev/null
          ForwardAgent yes
          Port 22
        """.stripIndent()

        writeFile file: WORKSPACE + '/local.conf', text: "${LOCAL_CONF}"

        writeFile file: WORKSPACE + '/sckrips.sh', text: """\
          #!/bin/bash -x
	  set -e
	  useradd -s /bin/bash -d /opt/stack -m stack
	  sed -i "s/devstack-generic/$ENV_NAME/g" /etc/hosts
	  hostname $ENV_NAME
	  echo "stack:cubswin:)" |  chpasswd
	  git clone $DEVSTACK_REPO -b $DEVSTACK_BRANCH /opt/stack/devstack
	  chown -R stack:stack /opt/stack/devstack
        """.stripIndent()

        sh "chmod +x ${WORKSPACE}/sckrips.sh"

        withEnv(["SSHPASS=${SSHPASS}"]) {

          sh "sshpass  -e scp -qF \"${WORKSPACE}/ssh-config\" ./sckrips.sh cirros@${ENV_IP}:."
          sh "sshpass -e ssh -F \"${WORKSPACE}/ssh-config\" cirros@${ENV_IP} ./sckrips.sh"
          sh "sshpass -e scp -qF \"${WORKSPACE}/ssh-config\" ./local.conf stack@${ENV_IP}:/opt/stack/devstack"
          sh "sshpass -e ssh -F \"${WORKSPACE}/ssh-config\" stack@${ENV_IP} cd devstack; ./stack.sh; exit"
       }
 //    }
 }
